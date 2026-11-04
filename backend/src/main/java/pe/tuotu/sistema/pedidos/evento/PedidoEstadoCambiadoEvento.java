package pe.tuotu.sistema.pedidos.evento;

import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;

/** Evento que se publica cuando un pedido cambia de estado. Cualquier observador interesado puede suscribirse. */
public record PedidoEstadoCambiadoEvento(Long pedidoId, String codigo, EstadoPedido anterior, EstadoPedido nuevo) {
}
