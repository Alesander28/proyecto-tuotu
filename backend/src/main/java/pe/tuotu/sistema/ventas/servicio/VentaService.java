package pe.tuotu.sistema.ventas.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;
import pe.tuotu.sistema.pedidos.repositorio.PedidoRepository;
import pe.tuotu.sistema.pedidos.servicio.PedidoMapper;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import pe.tuotu.sistema.ventas.dominio.Comprobante;
import pe.tuotu.sistema.ventas.dominio.Venta;
import pe.tuotu.sistema.ventas.dto.VentaDto;
import pe.tuotu.sistema.ventas.dto.VentaRegistrarRequest;
import pe.tuotu.sistema.ventas.evento.VentaRegistradaEvento;
import pe.tuotu.sistema.ventas.repositorio.ComprobanteRepository;
import pe.tuotu.sistema.ventas.repositorio.VentaRepository;

import java.util.List;

/** HU-009 (registrar ventas) y emisión de la boleta de consumo (HU-014). */
@Service
public class VentaService {

    private final VentaRepository ventas;
    private final ComprobanteRepository comprobantes;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final List<String> metodosPago;
    private final String serie;
    private final ApplicationEventPublisher publicador;

    public VentaService(VentaRepository ventas, ComprobanteRepository comprobantes, PedidoRepository pedidos,
                        UsuarioRepository usuarios,
                        @Value("${app.metodos-pago}") List<String> metodosPago,
                        @Value("${app.comprobante.serie}") String serie,
                        ApplicationEventPublisher publicador) {
        this.ventas = ventas;
        this.comprobantes = comprobantes;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.metodosPago = metodosPago;
        this.serie = serie;
        this.publicador = publicador;
    }

    public List<String> metodosPago() {
        return metodosPago;
    }

    /** Pedidos atendidos que todavía no se han cobrado. */
    @Transactional(readOnly = true)
    public List<PedidoDto> pendientesDeCobro() {
        return ventas.pedidosSinVenta(EstadoPedido.ATENDIDO).stream().map(PedidoMapper::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<VentaDto> recientes() {
        return ventas.findTop100ByOrderByFechaHoraDesc().stream().map(v -> aDto(v, null)).toList();
    }

    /**
     * Cobra un pedido. Solo se cobran pedidos atendidos y cada pedido tiene una sola venta. La venta y su boleta
     * se guardan juntas: si algo falla, no queda ninguna de las dos.
     */
    @Transactional
    public VentaDto registrar(VentaRegistrarRequest solicitud, String nombreCajero) {
        Usuario cajero = usuarios.findByNombreUsuario(nombreCajero)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión."));
        Pedido pedido = pedidos.findById(solicitud.pedidoId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El pedido no existe."));
        if (pedido.getEstado() != EstadoPedido.ATENDIDO) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se pueden cobrar pedidos atendidos. El pedido "
                    + pedido.getCodigo() + " está " + pedido.getEstado().getTexto() + ".");
        }
        if (ventas.existsByPedidoId(pedido.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "El pedido " + pedido.getCodigo() + " ya fue cobrado.");
        }
        String metodo = metodosPago.stream().filter(m -> m.equalsIgnoreCase(solicitud.metodoPago().trim()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Método de pago no válido. Opciones: " + String.join(", ", metodosPago) + "."));

        Venta venta = ventas.save(new Venta(pedido, cajero, metodo, pedido.calcularTotal()));
        Comprobante comprobante = comprobantes.save(new Comprobante(venta));
        comprobante.setNumero(String.format("%s-%08d", serie, comprobante.getId()));
        publicador.publishEvent(new VentaRegistradaEvento(venta.getId(), pedido.getCodigo(), venta.getTotal(),
                venta.getMetodoPago(), cajero.getNombreCompleto()));
        return aDto(venta, comprobante);
    }

    private VentaDto aDto(Venta v, Comprobante c) {
        return new VentaDto(v.getId(), v.getPedido().getCodigo(), v.getPedido().getMesa(), v.getFechaHora(),
                v.getMetodoPago(), v.getTotal(), v.getCajero().getNombreCompleto(),
                c == null ? null : c.getId(), c == null ? null : c.getNumero());
    }
}
