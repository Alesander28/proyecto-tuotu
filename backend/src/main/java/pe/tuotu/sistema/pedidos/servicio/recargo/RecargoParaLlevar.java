package pe.tuotu.sistema.pedidos.servicio.recargo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;

import java.math.BigDecimal;

/** Estrategia: cargo por empaque cuando el producto es para llevar. */
@Component
public class RecargoParaLlevar implements RecargoPedido {

    private final BigDecimal monto;

    public RecargoParaLlevar(@Value("${app.recargo.para-llevar}") BigDecimal monto) {
        this.monto = monto;
    }

    @Override
    public BigDecimal porUnidad(ItemPedidoRequest item) {
        return item.paraLlevar() ? monto : BigDecimal.ZERO;
    }
}
