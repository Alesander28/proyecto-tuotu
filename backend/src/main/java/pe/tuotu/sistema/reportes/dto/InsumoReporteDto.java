package pe.tuotu.sistema.reportes.dto;

import java.math.BigDecimal;

public record InsumoReporteDto(String insumo, String unidad, BigDecimal stockActual, BigDecimal entradas, BigDecimal salidas) {
}
