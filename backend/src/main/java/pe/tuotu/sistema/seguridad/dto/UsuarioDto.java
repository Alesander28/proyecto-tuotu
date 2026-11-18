package pe.tuotu.sistema.seguridad.dto;

import java.util.List;

public record UsuarioDto(
        Long id,
        String nombreUsuario,
        String nombreCompleto,
        boolean activo,
        Long rolId,
        String rol,
        List<String> permisos) {
}
