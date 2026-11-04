package pe.tuotu.sistema.pedidos.servicio.recargo;

import org.junit.jupiter.api.Test;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas de cada estrategia de recargo por separado (el patrón Strategy permite probarlas de forma aislada). */
class RecargoPedidoTest {

    private final RecargoExtraQueso extraQueso = new RecargoExtraQueso(new BigDecimal("3.00"));
    private final RecargoParaLlevar paraLlevar = new RecargoParaLlevar(new BigDecimal("2.00"));

    private ItemPedidoRequest item(boolean conExtraQueso, boolean conParaLlevar) {
        return new ItemPedidoRequest(1L, 1, conParaLlevar, conExtraQueso);
    }

    @Test
    void elExtraQuesoSoloSeCobraSiSeEligio() {
        assertEquals(0, new BigDecimal("3.00").compareTo(extraQueso.porUnidad(item(true, false))));
        assertEquals(0, BigDecimal.ZERO.compareTo(extraQueso.porUnidad(item(false, true))));
    }

    @Test
    void elEmpaqueParaLlevarSoloSeCobraSiSeEligio() {
        assertEquals(0, new BigDecimal("2.00").compareTo(paraLlevar.porUnidad(item(false, true))));
        assertEquals(0, BigDecimal.ZERO.compareTo(paraLlevar.porUnidad(item(true, false))));
    }

    @Test
    void unaEstrategiaNuevaPuedeEscribirseComoUnaLambda() {
        RecargoPedido delivery = ignorado -> new BigDecimal("1.50");

        assertEquals(0, new BigDecimal("1.50").compareTo(delivery.porUnidad(item(false, false))));
    }
}
