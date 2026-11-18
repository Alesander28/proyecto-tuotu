package pe.tuotu.sistema.pedidos.servicio.recargo;

import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;

import java.math.BigDecimal;

/** Estrategia: cargo por empaque cuando el producto es para llevar. Su monto lo define la categoría de la carta. */
@Component
public class RecargoParaLlevar implements RecargoPedido {

    @Override
    public BigDecimal porUnidad(ItemPedidoRequest item, Presentacion presentacion) {
        return item.paraLlevar() ? presentacion.getProducto().getCategoria().getRecargoParaLlevar() : BigDecimal.ZERO;
    }
}
