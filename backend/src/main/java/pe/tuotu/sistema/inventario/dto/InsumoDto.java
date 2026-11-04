package pe.tuotu.sistema.inventario.dto;

import java.math.BigDecimal;

public record InsumoDto(Long id, String nombre, String unidadMedida, BigDecimal stockActual) {
}
