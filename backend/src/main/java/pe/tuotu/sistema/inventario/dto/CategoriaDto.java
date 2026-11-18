package pe.tuotu.sistema.inventario.dto;

import java.math.BigDecimal;

public record CategoriaDto(Long id, String nombre, BigDecimal recargoParaLlevar) {
}
