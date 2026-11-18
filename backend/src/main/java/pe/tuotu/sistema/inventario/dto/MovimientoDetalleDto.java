package pe.tuotu.sistema.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDetalleDto(Long id, Long insumoId, String insumo, String unidad, String tipo, BigDecimal cantidad,
                                   LocalDateTime fechaHora, String motivo, String responsable) {
}
