package pe.tuotu.sistema.pedidos.servicio.recargo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;

import java.math.BigDecimal;

/** Estrategia: cargo por pedir extra queso. */
@Component
public class RecargoExtraQueso implements RecargoPedido {

    private final BigDecimal monto;

    public RecargoExtraQueso(@Value("${app.recargo.extra-queso}") BigDecimal monto) {
        this.monto = monto;
    }

    @Override
    public BigDecimal porUnidad(ItemPedidoRequest item) {
        return item.extraQueso() ? monto : BigDecimal.ZERO;
    }
}
