package pe.tuotu.sistema.seguridad.servicio;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.dto.RolDto;
import pe.tuotu.sistema.seguridad.dto.UsuarioActualizarRequest;
import pe.tuotu.sistema.seguridad.dto.UsuarioCrearRequest;
import pe.tuotu.sistema.seguridad.dto.UsuarioDto;
import pe.tuotu.sistema.seguridad.repositorio.RolRepository;
import pe.tuotu.sistema.seguridad.repositorio.SesionTokenRepository;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import java.util.List;

/** HU-002 (registrar), HU-003 (roles), HU-004 (actualizar) y HU-005 (desactivar). */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final SesionTokenRepository sesiones;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles,
                          SesionTokenRepository sesiones, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.sesiones = sesiones;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarios.findAll(Sort.by("nombreUsuario")).stream().map(UsuarioMapper::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RolDto> listarRoles() {
        return roles.findAll(Sort.by("nombre")).stream().map(UsuarioMapper::aDto).toList();
    }

    @Transactional
    public UsuarioDto crear(UsuarioCrearRequest solicitud) {
        String nombreUsuario = solicitud.nombreUsuario().trim();
        if (usuarios.existsByNombreUsuario(nombreUsuario)) {
            throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya existe.");
        }
        Rol rol = buscarRol(solicitud.rolId());
        Usuario usuario = new Usuario(nombreUsuario, encoder.encode(solicitud.contrasena()),
                solicitud.nombreCompleto().trim(), rol);
        return UsuarioMapper.aDto(usuarios.save(usuario));
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioActualizarRequest solicitud) {
        Usuario usuario = buscar(id);
        usuario.setNombreCompleto(solicitud.nombreCompleto().trim());
        usuario.setRol(buscarRol(solicitud.rolId()));
        String contrasena = solicitud.contrasena();
        if (contrasena != null && !contrasena.isBlank()) {
            if (contrasena.length() < 6) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 6 caracteres.");
            }
            usuario.setContrasenaHash(encoder.encode(contrasena));
        }
        return UsuarioMapper.aDto(usuarios.save(usuario));
    }

    /**
     * Desactiva la cuenta (no se borra, para conservar el historial) y revoca sus sesiones abiertas,
     * de modo que el acceso se bloquea de inmediato.
     */
    @Transactional
    public UsuarioDto desactivar(Long id, String nombreUsuarioActual) {
        Usuario usuario = buscar(id);
        if (usuario.getNombreUsuario().equals(nombreUsuarioActual)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puede desactivar su propia cuenta.");
        }
        usuario.setActivo(false);
        usuarios.save(usuario);
        sesiones.revocarPorUsuario(usuario.getId());
        return UsuarioMapper.aDto(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El usuario no existe."));
    }

    private Rol buscarRol(Long id) {
        return roles.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El rol elegido no existe."));
    }
}
