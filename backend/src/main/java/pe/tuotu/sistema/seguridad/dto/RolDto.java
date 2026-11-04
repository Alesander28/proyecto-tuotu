package pe.tuotu.sistema.seguridad.dto;

import java.util.List;

public record RolDto(Long id, String nombre, List<String> permisos) {
}
