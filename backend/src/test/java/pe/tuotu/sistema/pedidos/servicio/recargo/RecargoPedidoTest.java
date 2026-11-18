package pe.tuotu.sistema.pedidos.servicio.recargo;

import org.junit.jupiter.api.Test;
import pe.tuotu.sistema.pedidos.dominio.Categoria;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas de cada estrategia de recargo por separado (el patrón Strategy permite probarlas de forma aislada). */
class RecargoPedidoTest {

    private final RecargoExtraQueso extraQueso = new RecargoExtraQueso();
    private final RecargoParaLlevar paraLlevar = new RecargoParaLlevar();

    private Presentacion pizza(String nombre, String precio, String recargoQueso) {
        Producto producto = new Producto("Hawaiana", new Categoria("Pizzas", new BigDecimal("2.00")));
        return new Presentacion(nombre, new BigDecimal(precio), producto, new BigDecimal(recargoQueso));
    }

    private Presentacion pasta() {
        Producto producto = new Producto("Fetuchini al pesto", new Categoria("Pastas", new BigDecimal("1.50")));
        return new Presentacion("Único", new BigDecimal("25.00"), producto);
    }

    private ItemPedidoRequest item(boolean conExtraQueso, boolean conParaLlevar) {
        return new ItemPedidoRequest(1L, 1, conParaLlevar, conExtraQueso);
    }

    @Test
    void elExtraQuesoDependeDelTamanoYSoloSeCobraSiSeEligio() {
        assertEquals(0, new BigDecimal("3.00").compareTo(extraQueso.porUnidad(item(true, false), pizza("Personal", "22.00", "3.00"))));
        assertEquals(0, new BigDecimal("5.00").compareTo(extraQueso.porUnidad(item(true, false), pizza("Familiar", "42.00", "5.00"))));
        assertEquals(0, BigDecimal.ZERO.compareTo(extraQueso.porUnidad(item(false, true), pizza("Personal", "22.00", "3.00"))));
    }

    @Test
    void elEmpaqueParaLlevarDependeDeLaCategoria() {
        assertEquals(0, new BigDecimal("2.00").compareTo(paraLlevar.porUnidad(item(false, true), pizza("Personal", "22.00", "3.00"))));
        assertEquals(0, new BigDecimal("1.50").compareTo(paraLlevar.porUnidad(item(false, true), pasta())));
        assertEquals(0, BigDecimal.ZERO.compareTo(paraLlevar.porUnidad(item(true, false), pasta())));
    }

    @Test
    void unaEstrategiaNuevaPuedeEscribirseComoUnaLambda() {
        RecargoPedido delivery = (ignorado, otroIgnorado) -> new BigDecimal("1.50");

        assertEquals(0, new BigDecimal("1.50").compareTo(delivery.porUnidad(item(false, false), pasta())));
    }
}
