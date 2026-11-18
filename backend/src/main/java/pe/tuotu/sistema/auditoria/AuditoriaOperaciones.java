package pe.tuotu.sistema.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.auditoria.dominio.RegistroAuditoria;
import pe.tuotu.sistema.auditoria.evento.OperacionAuditada;
import pe.tuotu.sistema.auditoria.repositorio.AuditoriaRepository;
import pe.tuotu.sistema.pedidos.evento.PedidoEstadoCambiadoEvento;
import pe.tuotu.sistema.ventas.evento.VentaRegistradaEvento;

/**
 * Patrón Observer: observador que deja constancia, en la bitácora y en la base de datos, de lo que ocurre en el
 * sistema. Los servicios que publican los eventos no lo conocen, de modo que se pueden agregar otros observadores
 * (por ejemplo, una pantalla para la cocina) sin modificar {@code PedidoService} ni {@code VentaService}.
 *
 * <p>Los eventos son síncronos y locales al proceso: se guardan en la misma transacción que la operación que los
 * originó, de modo que si la operación falla no queda registro de algo que no ocurrió.
 */
@Component
public class AuditoriaOperaciones {

    private static final Logger BITACORA = LoggerFactory.getLogger("bitacora");

    private final AuditoriaRepository auditoria;

    public AuditoriaOperaciones(AuditoriaRepository auditoria) {
        this.auditoria = auditoria;
    }

    @EventListener
    public void alCambiarElEstadoDeUnPedido(PedidoEstadoCambiadoEvento evento) {
        registrar(evento.usuario(), "Pedidos", "CAMBIO_ESTADO_PEDIDO",
                "Pedido " + evento.codigo() + ": " + evento.anterior().getTexto() + " → " + evento.nuevo().getTexto(),
                evento.codigo());
    }

    @EventListener
    public void alRegistrarUnaVenta(VentaRegistradaEvento evento) {
        registrar(evento.cajero(), "Ventas", "VENTA_REGISTRADA",
                "Venta del pedido " + evento.pedidoCodigo() + " por S/ " + evento.total() + " (" + evento.metodoPago() + ")",
                evento.pedidoCodigo());
    }

    @EventListener
    public void alAuditarUnaOperacion(OperacionAuditada evento) {
        registrar(evento.usuario(), evento.modulo(), evento.tipo(), evento.descripcion(), evento.referencia());
    }

    private void registrar(String usuario, String modulo, String tipo, String descripcion, String referencia) {
        BITACORA.info("[{}] {} - {} ({})", modulo, tipo, descripcion, usuario);
        auditoria.save(new RegistroAuditoria(usuario, modulo, tipo, descripcion, referencia));
    }
}
