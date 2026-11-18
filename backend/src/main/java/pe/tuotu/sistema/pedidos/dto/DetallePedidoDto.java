package pe.tuotu.sistema.pedidos.dto;

import java.math.BigDecimal;

public record DetallePedidoDto(
        String producto,
        String presentacion,
        int cantidad,
        BigDecimal precioUnitario,
        boolean paraLlevar,
        boolean extraQueso,
        BigDecimal subtotal) {
}
