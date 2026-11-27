package pe.tuotu.sistema.seguridad.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.SesionToken;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.dto.LoginRequest;
import pe.tuotu.sistema.seguridad.dto.LoginResponse;
import pe.tuotu.sistema.seguridad.repositorio.SesionTokenRepository;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import pe.tuotu.sistema.seguridad.util.TokenUtil;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private SesionTokenRepository sesiones;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private ApplicationEventPublisher publicador;
    @InjectMocks
    private AuthService servicio;

    private Usuario usuarioMozo() {
        Rol rol = new Rol("MOZO", new HashSet<>());
        ReflectionTestUtils.setField(rol, "id", 2L);
        Usuario usuario = new Usuario("mozo01", "hash", "Mozo Uno", rol);
        ReflectionTestUtils.setField(usuario, "id", 5L);
        return usuario;
    }

    @Test
    void loginCorrectoDevuelveTokenYGuardaLaSesion() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(usuarioMozo()));
        when(encoder.matches("clave123", "hash")).thenReturn(true);

        LoginResponse respuesta = servicio.login(new LoginRequest("mozo01", "clave123"), "127.0.0.1");

        assertNotNull(respuesta.token());
        assertEquals("MOZO", respuesta.usuario().rol());
        verify(sesiones).save(any(SesionToken.class));
    }

    @Test
    void contrasenaIncorrectaDevuelveMensajeUnico() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(usuarioMozo()));
        when(encoder.matches("mala", "hash")).thenReturn(false);

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("mozo01", "mala"), "127.0.0.1"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getEstado());
        assertEquals("Usuario o contraseña incorrectos", error.getMessage());
        verify(sesiones, never()).save(any());
    }

    @Test
    void usuarioInexistenteDevuelveElMismoMensaje() {
        when(usuarios.findByNombreUsuario("nadie")).thenReturn(Optional.empty());
        when(encoder.matches(any(), any())).thenReturn(false);

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("nadie", "clave123"), "127.0.0.1"));

        assertEquals("Usuario o contraseña incorrectos", error.getMessage());
    }

    @Test
    void usuarioDesactivadoNoPuedeIngresar() {
        Usuario usuario = usuarioMozo();
        usuario.setActivo(false);
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(usuario));
        when(encoder.matches("clave123", "hash")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("mozo01", "clave123"), "127.0.0.1"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getEstado());
        verify(encoder).matches("clave123", "hash");
        verify(sesiones, never()).save(any());
    }

    @Test
    void bloqueaDespuesDeTresIntentosFallidos() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(usuarioMozo()));
        when(encoder.matches("incorrecta", "hash")).thenReturn(false);
        for (int i = 0; i < 3; i++) {
            assertThrows(ApiException.class, () -> servicio.login(new LoginRequest("mozo01", "incorrecta"), "10.0.0.1"));
        }
        ApiException bloqueo = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("mozo01", "incorrecta"), "10.0.0.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, bloqueo.getEstado());
    }


    @Test
    void bloqueoSeLevantaDespuesDeCincoMinutos() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(usuarioMozo()));
        when(encoder.matches("incorrecta", "hash")).thenReturn(false);

        for (int i = 0; i < 3; i++) {
            assertThrows(ApiException.class,
                    () -> servicio.login(new LoginRequest("mozo01", "incorrecta"), "10.0.0.5"));
        }

        ApiException bloqueo = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("mozo01", "incorrecta"), "10.0.0.5"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, bloqueo.getEstado());

        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentHashMap<String, Object> controles =
                (java.util.concurrent.ConcurrentHashMap<String, Object>)
                        ReflectionTestUtils.getField(servicio, "intentos");

        Object control = controles.get("usuario:mozo01|ip:10.0.0.5");
        ReflectionTestUtils.setField(control, "inicio", System.currentTimeMillis() - 5 * 60 * 1000L);

        ApiException nuevoIntento = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("mozo01", "incorrecta"), "10.0.0.5"));
        assertEquals(HttpStatus.UNAUTHORIZED, nuevoIntento.getEstado());
    }


    @Test
    void limitaIntentosPorIpAunqueCambieElNombreDeUsuario() {
        when(usuarios.findByNombreUsuario(any())).thenReturn(Optional.empty());
        when(encoder.matches(any(), any())).thenReturn(false);
        for (int i = 1; i <= 15; i++) {
            String nombre = "nadie" + i;
            assertThrows(ApiException.class,
                    () -> servicio.login(new LoginRequest(nombre, "incorrecta"), "192.0.2.10"));
        }
        ApiException bloqueo = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("nadie16", "incorrecta"), "192.0.2.10"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, bloqueo.getEstado());
    }

    @Test
    void cerrarSesionRevocaElToken() {
        SesionToken sesion = new SesionToken("hash-del-token", usuarioMozo(), java.time.LocalDateTime.now().plusHours(8));
        when(sesiones.findByTokenHashAndRevocadoFalseAndExpiraEnAfter(
                eq(TokenUtil.hash("token-de-prueba")),
                any(java.time.LocalDateTime.class)))
                .thenReturn(Optional.of(sesion));

        servicio.logout("token-de-prueba");

        assertTrue(sesion.isRevocado());
        verify(sesiones).save(sesion);
    }
}
