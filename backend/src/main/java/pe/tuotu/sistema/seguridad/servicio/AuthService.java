package pe.tuotu.sistema.seguridad.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public AuthService(UsuarioRepository usuarios, SesionTokenRepository sesiones, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.encoder = encoder;
    }

    /**
     * Valida las credenciales. Siempre responde con el mismo mensaje si algo falla (usuario inexistente,
     * contraseña errónea o cuenta desactivada), para no revelar cuál dato falló.
     */
    @Transactional
    public LoginResponse login(LoginRequest solicitud) {
        Usuario usuario = usuarios.findByNombreUsuario(solicitud.nombreUsuario())
                .orElseThrow(AuthService::credencialesInvalidas);
        if (!usuario.isActivo() || !encoder.matches(solicitud.contrasena(), usuario.getContrasenaHash())) {
            throw credencialesInvalidas();
        }
        String token = TokenUtil.generar();
        sesiones.save(new SesionToken(TokenUtil.hash(token), usuario));
        return new LoginResponse(token, UsuarioMapper.aDto(usuario));
    }

    @Transactional
    public void logout(String token) {
        sesiones.findByTokenHashAndRevocadoFalse(TokenUtil.hash(token)).ifPresent(sesion -> {
            sesion.setRevocado(true);
            sesiones.save(sesion);
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
