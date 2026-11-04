package pe.tuotu.sistema.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDto(
        Long id,
        String codigo,
        String mesa,
        LocalDateTime fechaHora,
        String estado,
        String mozo,
        List<DetallePedidoDto> detalles,
        BigDecimal total) {
}
