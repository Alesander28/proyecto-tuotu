package pe.tuotu.sistema.seguridad.servicio;

import pe.tuotu.sistema.seguridad.dominio.Permiso;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.dto.RolDto;
import pe.tuotu.sistema.seguridad.dto.UsuarioDto;
import java.util.List;

/** Convierte entidades en objetos de transferencia (nunca se expone el hash de la contraseña). */
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombreUsuario(), u.getNombreCompleto(), u.isActivo(),
                u.getRol().getId(), u.getRol().getNombre(), codigos(u.getRol()));
    }

    public static RolDto aDto(Rol r) {
        return new RolDto(r.getId(), r.getNombre(), codigos(r));
    }

    private static List<String> codigos(Rol rol) {
        return rol.getPermisos().stream().map(Permiso::getCodigo).sorted().toList();
    }
}
