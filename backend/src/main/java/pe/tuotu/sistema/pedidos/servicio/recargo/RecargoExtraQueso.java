package pe.tuotu.sistema.pedidos.servicio.recargo;

import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;

import java.math.BigDecimal;

/** Estrategia: cargo por pedir extra queso. Su monto lo define cada presentación (por ejemplo, personal o familiar). */
@Component
public class RecargoExtraQueso implements RecargoPedido {

    @Override
    public BigDecimal porUnidad(ItemPedidoRequest item, Presentacion presentacion) {
        return item.extraQueso() && presentacion.admiteExtraQueso() ? presentacion.getRecargoExtraQueso() : BigDecimal.ZERO;
    }
}
