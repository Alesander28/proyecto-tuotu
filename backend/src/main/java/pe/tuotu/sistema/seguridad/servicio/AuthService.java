package pe.tuotu.sistema.seguridad.servicio;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;
import pe.tuotu.sistema.auditoria.evento.OperacionAuditada;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.seguridad.dominio.SesionToken;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.dto.LoginRequest;
import pe.tuotu.sistema.seguridad.dto.LoginResponse;
import pe.tuotu.sistema.seguridad.dto.UsuarioDto;
import pe.tuotu.sistema.seguridad.repositorio.SesionTokenRepository;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import pe.tuotu.sistema.seguridad.util.TokenUtil;

/** HU-001 (iniciar sesión) y HU-006 (cerrar sesión). */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final SesionTokenRepository sesiones;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher publicador;
    private final ConcurrentHashMap<String, Intentos> intentos = new ConcurrentHashMap<>();
    private static final int MAX_INTENTOS_USUARIO_IP = 3;
    // Evita que una sola persona bloquee a todos los usuarios detrás de la misma red.
    private static final int MAX_INTENTOS_IP = 15;
    private static final long BLOQUEO_MS = 15 * 60 * 1000L;
    private static final long VIGENCIA_HORAS = 8;
    // Hash ficticio para ejecutar BCrypt también cuando el usuario no existe.
    private static final String HASH_USUARIO_INEXISTENTE = new BCryptPasswordEncoder().encode("clave-ficticia-no-utilizable");
    private final AtomicInteger solicitudes = new AtomicInteger();

    private static class Intentos {
        private final AtomicInteger cantidad = new AtomicInteger();
        private volatile long inicio = System.currentTimeMillis();
    }

    public AuthService(UsuarioRepository usuarios, SesionTokenRepository sesiones, PasswordEncoder encoder,
                       ApplicationEventPublisher publicador) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.encoder = encoder;
        this.publicador = publicador;
    }

    /**
     * Valida las credenciales. Siempre responde con el mismo mensaje si algo falla (usuario inexistente,
     * contraseña errónea o cuenta desactivada), para no revelar cuál dato falló.
     */
    @Transactional
    public LoginResponse login(LoginRequest solicitud, String origen) {
        String nombre = solicitud.nombreUsuario().trim();
        String ip = (origen == null || origen.isBlank()) ? "desconocido" : origen;
        String llaveUsuarioIp = "usuario:" + nombre.toLowerCase(java.util.Locale.ROOT) + "|ip:" + ip;
        String llaveIp = "ip:" + ip;
        long ahoraMs = System.currentTimeMillis();

        // Limpieza ocasional para que intentos antiguos no acumulen claves indefinidamente.
        if (solicitudes.incrementAndGet() % 256 == 0) {
            intentos.entrySet().removeIf(e -> ahoraMs - e.getValue().inicio >= BLOQUEO_MS);
        }

        Intentos controlUsuario = intentos.computeIfAbsent(llaveUsuarioIp, k -> new Intentos());
        Intentos controlIp = intentos.computeIfAbsent(llaveIp, k -> new Intentos());
        reiniciarSiVencio(controlUsuario, ahoraMs);
        reiniciarSiVencio(controlIp, ahoraMs);
        if (estaBloqueado(controlUsuario, MAX_INTENTOS_USUARIO_IP)
                || estaBloqueado(controlIp, MAX_INTENTOS_IP)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos. Espere 15 minutos antes de volver a intentar.");
        }

        Usuario usuario = usuarios.findByNombreUsuario(nombre).orElse(null);
        String hash = usuario == null ? HASH_USUARIO_INEXISTENTE : usuario.getContrasenaHash();
        // No revelar si el usuario existe o está desactivado mediante diferencias de tiempo.
        boolean contrasenaCorrecta = solicitud.contrasena().getBytes(StandardCharsets.UTF_8).length <= 72
                && encoder.matches(solicitud.contrasena(), hash);
        if (usuario == null || !usuario.isActivo() || !contrasenaCorrecta) {
            registrarFallo(controlUsuario, ahoraMs, MAX_INTENTOS_USUARIO_IP);
            registrarFallo(controlIp, ahoraMs, MAX_INTENTOS_IP);
            throw credencialesInvalidas();
        }

        intentos.remove(llaveUsuarioIp);
        intentos.remove(llaveIp);
        String token = TokenUtil.generar();
        LocalDateTime expiraEn = LocalDateTime.now().plusHours(VIGENCIA_HORAS);
        sesiones.save(new SesionToken(TokenUtil.hash(token), usuario, expiraEn));
        publicador.publishEvent(new OperacionAuditada("Seguridad", "INICIO_SESION", usuario.getNombreUsuario(),
                "Inicio de sesión", usuario.getNombreUsuario()));
        return new LoginResponse(token, UsuarioMapper.aDto(usuario), expiraEn.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    private static void reiniciarSiVencio(Intentos control, long ahoraMs) {
        if (ahoraMs - control.inicio >= BLOQUEO_MS) {
            control.inicio = ahoraMs;
            control.cantidad.set(0);
        }
    }

    private static boolean estaBloqueado(Intentos control, int limite) {
        return control.cantidad.get() >= limite;
    }

    private static void registrarFallo(Intentos control, long ahoraMs, int limite) {
        if (control.cantidad.incrementAndGet() >= limite) control.inicio = ahoraMs;
    }

    @Transactional
    public void logout(String token) {
        sesiones.findByTokenHashAndRevocadoFalseAndExpiraEnAfter(TokenUtil.hash(token), LocalDateTime.now()).ifPresent(sesion -> {
            sesion.setRevocado(true);
            sesiones.save(sesion);
            publicador.publishEvent(new OperacionAuditada("Seguridad", "CIERRE_SESION", sesion.getUsuario().getNombreUsuario(),
                    "Cierre de sesión", sesion.getUsuario().getNombreUsuario()));
        });
    }

    @Transactional(readOnly = true)
    public UsuarioDto actual(String nombreUsuario) {
        Usuario usuario = usuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión."));
        return UsuarioMapper.aDto(usuario);
    }

    private static ApiException credencialesInvalidas() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }
}
