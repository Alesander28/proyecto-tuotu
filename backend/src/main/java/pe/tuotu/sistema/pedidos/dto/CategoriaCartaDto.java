package pe.tuotu.sistema.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoriaCartaDto(Long id, String nombre, BigDecimal recargoParaLlevar, List<ProductoCartaDto> productos) {
}
