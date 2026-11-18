package pe.tuotu.sistema.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaDto(
        Long id,
        String pedidoCodigo,
        String mesa,
        LocalDateTime fechaHora,
        String metodoPago,
        BigDecimal total,
        String cajero,
        Long comprobanteId,
        String comprobanteNumero) {
}
