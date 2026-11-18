package pe.tuotu.sistema.pedidos.servicio;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.auditoria.evento.OperacionAuditada;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.pedidos.dominio.DetallePedido;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;
import pe.tuotu.sistema.pedidos.dto.PedidoRegistrarRequest;
import pe.tuotu.sistema.pedidos.evento.PedidoEstadoCambiadoEvento;
import pe.tuotu.sistema.pedidos.repositorio.PedidoRepository;
import pe.tuotu.sistema.pedidos.repositorio.PresentacionRepository;
import pe.tuotu.sistema.pedidos.servicio.recargo.RecargoPedido;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** HU-007 (registrar pedidos) y HU-008 (seguimiento del estado). */
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final PresentacionRepository presentaciones;
    private final UsuarioRepository usuarios;
    private final List<RecargoPedido> recargos;
    private final ApplicationEventPublisher publicador;

    /** Spring entrega aquí todas las estrategias de recargo que existan (Strategy) y el publicador de eventos (Observer). */
    public PedidoService(PedidoRepository pedidos, PresentacionRepository presentaciones, UsuarioRepository usuarios,
                         List<RecargoPedido> recargos, ApplicationEventPublisher publicador) {
        this.pedidos = pedidos;
        this.presentaciones = presentaciones;
        this.usuarios = usuarios;
        this.recargos = recargos;
        this.publicador = publicador;
    }

    /**
     * Registra el pedido de una mesa. Los precios no vienen de la pantalla: se leen de la carta en el servidor,
     * y se suman los cargos adicionales de cada estrategia de recargo. El pedido queda en estado PENDIENTE.
     */
    @Transactional
    public PedidoDto registrar(PedidoRegistrarRequest solicitud, String nombreMozo) {
        Usuario mozo = usuarios.findByNombreUsuario(nombreMozo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión."));
        Pedido pedido = new Pedido(solicitud.mesa().trim(), mozo);

        for (ItemPedidoRequest item : solicitud.items()) {
            Presentacion presentacion = presentaciones.findById(item.presentacionId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Hay un producto que no existe en la carta."));
            if (!presentacion.getProducto().isActivo()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "El producto «" + presentacion.getProducto().getNombre() + "» no está disponible.");
            }
            if (item.extraQueso() && !presentacion.admiteExtraQueso()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "«" + presentacion.getProducto().getNombre()
                        + "» no admite extra queso.");
            }
            BigDecimal precio = presentacion.getPrecio();
            for (RecargoPedido recargo : recargos) {
                precio = precio.add(recargo.porUnidad(item, presentacion));
            }
            pedido.agregarDetalle(new DetallePedido(presentacion, item.cantidad(), precio,
                    item.paraLlevar(), item.extraQueso()));
        }

        Pedido guardado = pedidos.save(pedido);
        guardado.setCodigo(String.format("%04d", guardado.getId()));
        publicador.publishEvent(new OperacionAuditada("Pedidos", "PEDIDO_REGISTRADO", nombreMozo,
                "Pedido " + guardado.getCodigo() + " (mesa " + guardado.getMesa() + ") por S/ " + guardado.calcularTotal(),
                guardado.getCodigo()));
        return PedidoMapper.aDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> listar(EstadoPedido estado) {
        List<Pedido> lista = estado == null
                ? pedidos.findTop100ByOrderByFechaHoraDesc()
                : pedidos.findTop100ByEstadoOrderByFechaHoraDesc(estado);
        return lista.stream().map(PedidoMapper::aDto).toList();
    }

    /**
     * Búsqueda con filtros para el gerente. Sin fechas, devuelve los últimos 30 días. El texto busca por número de
     * pedido o por mesa; el mozo, por su nombre o su usuario.
     */
    @Transactional(readOnly = true)
    public List<PedidoDto> buscar(LocalDate desde, LocalDate hasta, EstadoPedido estado, String mozo, String texto) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        if (fin.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la fecha inicial.");
        }
        String m = mozo == null ? "" : mozo.trim().toLowerCase(Locale.ROOT);
        String q = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return pedidos.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
                        inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()).stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> m.isEmpty() || p.getMozo().getNombreCompleto().toLowerCase(Locale.ROOT).contains(m)
                        || p.getMozo().getNombreUsuario().toLowerCase(Locale.ROOT).contains(m))
                .filter(p -> q.isEmpty() || (p.getCodigo() != null && p.getCodigo().contains(q))
                        || p.getMesa().toLowerCase(Locale.ROOT).contains(q))
                .limit(500)
                .map(PedidoMapper::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoDto obtener(Long id) {
        return PedidoMapper.aDto(buscar(id));
    }

    /** Un pedido solo avanza al estado siguiente: pendiente, en preparación y atendido, en ese orden. */
    @Transactional
    public PedidoDto cambiarEstado(Long id, EstadoPedido nuevo, String usuario) {
        Pedido pedido = buscar(id);
        EstadoPedido siguiente = pedido.getEstado().siguiente()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "El pedido ya fue atendido."));
        if (siguiente != nuevo) {
            throw new ApiException(HttpStatus.CONFLICT, "El pedido pasa de «" + pedido.getEstado().getTexto()
                    + "» a «" + siguiente.getTexto() + "». No se puede saltar ni retroceder de estado.");
        }
        EstadoPedido anterior = pedido.getEstado();
        pedido.setEstado(nuevo);
        publicador.publishEvent(new PedidoEstadoCambiadoEvento(pedido.getId(), pedido.getCodigo(), anterior, nuevo, usuario));
        return PedidoMapper.aDto(pedido);
    }

    private Pedido buscar(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El pedido no existe."));
    }
}
