package pe.tuotu.sistema.ventas.evento;

import java.math.BigDecimal;

/** Evento que se publica cuando se cobra un pedido. */
public record VentaRegistradaEvento(Long ventaId, String pedidoCodigo, BigDecimal total, String metodoPago, String cajero) {
}
