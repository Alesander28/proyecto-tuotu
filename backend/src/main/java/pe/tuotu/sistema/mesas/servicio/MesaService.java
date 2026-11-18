package pe.tuotu.sistema.mesas.servicio;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.auditoria.evento.OperacionAuditada;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.mesas.dominio.Mesa;
import pe.tuotu.sistema.mesas.dto.MesaDto;
import pe.tuotu.sistema.mesas.repositorio.MesaRepository;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;
import pe.tuotu.sistema.pedidos.repositorio.PedidoRepository;
import pe.tuotu.sistema.pedidos.servicio.PedidoMapper;
import pe.tuotu.sistema.ventas.repositorio.VentaRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Administración de las mesas y consulta de su ocupación. Solo el gerente. */
@Service
public class MesaService {

    private final MesaRepository mesas;
    private final PedidoRepository pedidos;
    private final VentaRepository ventas;
    private final ApplicationEventPublisher publicador;

    public MesaService(MesaRepository mesas, PedidoRepository pedidos, VentaRepository ventas,
                       ApplicationEventPublisher publicador) {
        this.mesas = mesas;
        this.pedidos = pedidos;
        this.ventas = ventas;
        this.publicador = publicador;
    }

    /** Pedidos sin cobrar: pendientes, en preparación y atendidos que todavía no tienen venta. */
    private List<Pedido> pedidosAbiertos() {
        List<Pedido> abiertos = new ArrayList<>(pedidos.findTop100ByEstadoOrderByFechaHoraDesc(EstadoPedido.PENDIENTE));
        abiertos.addAll(pedidos.findTop100ByEstadoOrderByFechaHoraDesc(EstadoPedido.EN_PREPARACION));
        abiertos.addAll(ventas.pedidosSinVenta(EstadoPedido.ATENDIDO));
        return abiertos;
    }

    @Transactional(readOnly = true)
    public List<MesaDto> listar() {
        List<Pedido> abiertos = pedidosAbiertos();
        LocalDateTime ahora = LocalDateTime.now();
        return mesas.findAllByOrderByNumeroAsc().stream().map(mesa -> {
            List<Pedido> delaMesa = abiertos.stream().filter(p -> p.getMesa().trim().equalsIgnoreCase(mesa.getNumero())).toList();
            if (delaMesa.isEmpty()) {
                return new MesaDto(mesa.getId(), mesa.getNumero(), mesa.isActiva(), "LIBRE", 0, null, null, null, null, null, BigDecimal.ZERO);
            }
            Pedido primero = delaMesa.stream().min(Comparator.comparing(Pedido::getFechaHora)).orElseThrow();
            BigDecimal total = delaMesa.stream().map(Pedido::calcularTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new MesaDto(mesa.getId(), mesa.getNumero(), mesa.isActiva(), "OCUPADA", delaMesa.size(), primero.getCodigo(),
                    primero.getEstado().name(), primero.getMozo().getNombreCompleto(), primero.getFechaHora(),
                    Duration.between(primero.getFechaHora(), ahora).toMinutes(), total);
        }).toList();
    }

    /** Últimos pedidos atendidos en la mesa. */
    @Transactional(readOnly = true)
    public List<PedidoDto> historial(Long id) {
        Mesa mesa = buscar(id);
        return pedidos.findTop50ByMesaOrderByFechaHoraDesc(mesa.getNumero()).stream().map(PedidoMapper::aDto).toList();
    }

    @Transactional
    public MesaDto crear(String numero, String usuario) {
        String limpio = numero.trim();
        if (mesas.existsByNumeroIgnoreCase(limpio)) {
            throw new ApiException(HttpStatus.CONFLICT, "La mesa «" + limpio + "» ya existe.");
        }
        Mesa guardada = mesas.save(new Mesa(limpio));
        publicador.publishEvent(new OperacionAuditada("Mesas", "MESA_CREADA", usuario, "Mesa " + limpio + " registrada", limpio));
        return new MesaDto(guardada.getId(), guardada.getNumero(), true, "LIBRE", 0, null, null, null, null, null, BigDecimal.ZERO);
    }

    @Transactional
    public void cambiarActiva(Long id, boolean activa, String usuario) {
        Mesa mesa = buscar(id);
        mesa.setActiva(activa);
        mesas.save(mesa);
        publicador.publishEvent(new OperacionAuditada("Mesas", activa ? "MESA_ACTIVADA" : "MESA_DESACTIVADA", usuario,
                "Mesa " + mesa.getNumero() + (activa ? " activada" : " desactivada"), mesa.getNumero()));
    }

    private Mesa buscar(Long id) {
        return mesas.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La mesa no existe."));
    }
}
