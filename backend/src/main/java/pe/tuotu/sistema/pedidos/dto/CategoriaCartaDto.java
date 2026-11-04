package pe.tuotu.sistema.pedidos.dto;

import java.util.List;

public record CategoriaCartaDto(Long id, String nombre, List<ProductoCartaDto> productos) {
}
