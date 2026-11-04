package pe.tuotu.sistema.seguridad.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private SesionTokenRepository sesiones;
    @Mock
    private PasswordEncoder encoder;
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

        LoginResponse respuesta = servicio.login(new LoginRequest("mozo01", "clave123"));

        assertNotNull(respuesta.token());
        assertEquals("MOZO", respuesta.usuario().rol());
        verify(sesiones).save(any(SesionToken.class));
    }

    @Test
    void contrasenaIncorrectaDevuelveMensajeUnico() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(usuarioMozo()));
        when(encoder.matches("mala", "hash")).thenReturn(false);

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("mozo01", "mala")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getEstado());
        assertEquals("Usuario o contraseña incorrectos", error.getMessage());
        verify(sesiones, never()).save(any());
    }

    @Test
    void usuarioInexistenteDevuelveElMismoMensaje() {
        when(usuarios.findByNombreUsuario("nadie")).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("nadie", "clave123")));

        assertEquals("Usuario o contraseña incorrectos", error.getMessage());
    }

    @Test
    void usuarioDesactivadoNoPuedeIngresar() {
        Usuario usuario = usuarioMozo();
        usuario.setActivo(false);
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(usuario));

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.login(new LoginRequest("mozo01", "clave123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getEstado());
        verifyNoInteractions(encoder);
        verify(sesiones, never()).save(any());
    }

    @Test
    void cerrarSesionRevocaElToken() {
        SesionToken sesion = new SesionToken("hash-del-token", usuarioMozo());
        when(sesiones.findByTokenHashAndRevocadoFalse(TokenUtil.hash("token-de-prueba")))
                .thenReturn(Optional.of(sesion));

        servicio.logout("token-de-prueba");

        assertTrue(sesion.isRevocado());
        verify(sesiones).save(sesion);
    }
}
