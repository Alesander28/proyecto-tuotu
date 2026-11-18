package pe.tuotu.sistema.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDto(Long id, String tipo, BigDecimal cantidad, LocalDateTime fechaHora, String motivo, String responsable) {
}
