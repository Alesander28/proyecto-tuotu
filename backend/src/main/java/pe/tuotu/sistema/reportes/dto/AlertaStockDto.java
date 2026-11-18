package pe.tuotu.sistema.reportes.dto;

import java.math.BigDecimal;

public record AlertaStockDto(String insumo, String unidad, BigDecimal stockActual, BigDecimal stockMinimo, String estado) {
}
