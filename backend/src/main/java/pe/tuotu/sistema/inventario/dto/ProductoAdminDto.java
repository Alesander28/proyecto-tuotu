package pe.tuotu.sistema.inventario.dto;

import pe.tuotu.sistema.pedidos.dto.PresentacionDto;

import java.util.List;

public record ProductoAdminDto(Long id, String nombre, boolean activo, Long categoriaId, String categoria,
                               List<PresentacionDto> presentaciones) {
}
