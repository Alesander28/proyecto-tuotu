package pe.tuotu.sistema.pedidos.servicio.recargo;

import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;

import java.math.BigDecimal;

/**
 * Patrón Strategy: cada cargo adicional del pedido (extra queso, para llevar, etc.) es una estrategia
 * independiente con el mismo contrato. Para agregar un cargo nuevo solo se crea una clase que implemente
 * esta interfaz; {@code PedidoService} no cambia.
 */
@FunctionalInterface
public interface RecargoPedido {

    /** Cargo adicional por unidad que corresponde al ítem (cero si no aplica). */
    BigDecimal porUnidad(ItemPedidoRequest item);
}
