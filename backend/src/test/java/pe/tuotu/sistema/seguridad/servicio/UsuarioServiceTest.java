package pe.tuotu.sistema.seguridad.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.dto.UsuarioCrearRequest;
import pe.tuotu.sistema.seguridad.dto.UsuarioDto;
import pe.tuotu.sistema.seguridad.repositorio.RolRepository;
import pe.tuotu.sistema.seguridad.repositorio.SesionTokenRepository;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private RolRepository roles;
    @Mock
    private SesionTokenRepository sesiones;
    @Mock
    private PasswordEncoder encoder;
    @InjectMocks
    private UsuarioService servicio;

    private Rol rolMozo() {
        Rol rol = new Rol("MOZO", new HashSet<>());
        ReflectionTestUtils.setField(rol, "id", 2L);
        return rol;
    }

    private Usuario usuario(String nombreUsuario, long id) {
        Usuario usuario = new Usuario(nombreUsuario, "hash", "Nombre Completo", rolMozo());
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    @Test
    void noPermiteNombreDeUsuarioRepetido() {
        when(usuarios.existsByNombreUsuario("mozo01")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.crear(new UsuarioCrearRequest("mozo01", "clave123", "Mozo Uno", 2L)));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(usuarios, never()).save(any());
    }

    @Test
    void guardaLaContrasenaCifrada() {
        when(usuarios.existsByNombreUsuario("mozo01")).thenReturn(false);
        when(roles.findById(2L)).thenReturn(Optional.of(rolMozo()));
        when(encoder.encode("clave123")).thenReturn("HASH");
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UsuarioDto dto = servicio.crear(new UsuarioCrearRequest("mozo01", "clave123", "Mozo Uno", 2L));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        assertEquals("HASH", captor.getValue().getContrasenaHash());
        assertEquals("mozo01", dto.nombreUsuario());
        assertEquals("MOZO", dto.rol());
    }

    @Test
    void desactivarBloqueaLaCuentaYRevocaSusSesiones() {
        Usuario usuario = usuario("mozo01", 5L);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));

        servicio.desactivar(5L, "gerente");

        assertFalse(usuario.isActivo());
        verify(sesiones).revocarPorUsuario(5L);
    }

    @Test
    void nadiePuedeDesactivarSuPropiaCuenta() {
        Usuario usuario = usuario("gerente", 1L);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        ApiException error = assertThrows(ApiException.class, () -> servicio.desactivar(1L, "gerente"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verify(sesiones, never()).revocarPorUsuario(any());
    }
}
