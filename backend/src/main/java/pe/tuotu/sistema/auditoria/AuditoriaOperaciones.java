package pe.tuotu.sistema.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.evento.PedidoEstadoCambiadoEvento;
import pe.tuotu.sistema.ventas.evento.VentaRegistradaEvento;

/**
 * Patrón Observer: observador que deja constancia en la bitácora de lo que ocurre en el sistema. Los servicios
 * que publican los eventos no lo conocen, de modo que se pueden agregar otros observadores (por ejemplo, una
 * pantalla para la cocina o estadísticas) sin modificar {@code PedidoService} ni {@code VentaService}.
 *
 * <p>Los eventos son síncronos y locales al proceso: no hay mensajería distribuida ni reintentos. Si un observador
 * lanzara una excepción, se interrumpiría la operación; por eso este solo escribe en el registro.
 */
@Component
public class AuditoriaOperaciones {

    private static final Logger BITACORA = LoggerFactory.getLogger("bitacora");

    @EventListener
    public void alCambiarElEstadoDeUnPedido(PedidoEstadoCambiadoEvento evento) {
        BITACORA.info("Pedido {}: {} -> {}", evento.codigo(), evento.anterior(), evento.nuevo());
    }

    @EventListener
    public void alRegistrarUnaVenta(VentaRegistradaEvento evento) {
        BITACORA.info("Venta del pedido {} por S/ {} ({}), cajero {}", evento.pedidoCodigo(), evento.total(),
                evento.metodoPago(), evento.cajero());
    }
}
