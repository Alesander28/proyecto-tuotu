package pe.tuotu.sistema.inventario.servicio;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.auditoria.evento.OperacionAuditada;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.inventario.dominio.Insumo;
import pe.tuotu.sistema.inventario.dominio.MovimientoInventario;
import pe.tuotu.sistema.inventario.dominio.TipoMovimiento;
import pe.tuotu.sistema.inventario.dto.InsumoCrearRequest;
import pe.tuotu.sistema.inventario.dto.InsumoDto;
import pe.tuotu.sistema.inventario.dto.MovimientoDetalleDto;
import pe.tuotu.sistema.inventario.dto.MovimientoDto;
import pe.tuotu.sistema.inventario.dto.MovimientoRequest;
import pe.tuotu.sistema.inventario.repositorio.InsumoRepository;
import pe.tuotu.sistema.inventario.repositorio.MovimientoRepository;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** HU-011 (entradas), HU-012 (salidas) y HU-013 (consulta del inventario). */
@Service
public class InsumoService {

    private final InsumoRepository insumos;
    private final MovimientoRepository movimientos;
    private final UsuarioRepository usuarios;
    private final ApplicationEventPublisher publicador;

    public InsumoService(InsumoRepository insumos, MovimientoRepository movimientos, UsuarioRepository usuarios,
                         ApplicationEventPublisher publicador) {
        this.insumos = insumos;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.publicador = publicador;
    }

    /** HU-013: lista el inventario con el stock actual; el texto filtra por nombre. */
    @Transactional(readOnly = true)
    public List<InsumoDto> listar(String texto) {
        String filtro = texto == null ? "" : texto.trim();
        return insumos.findByNombreContainingIgnoreCaseOrderByNombreAsc(filtro).stream()
                .map(InsumoService::aDto).toList();
    }

    @Transactional
    public InsumoDto crear(InsumoCrearRequest solicitud, String usuario) {
        String nombre = solicitud.nombre().trim();
        if (insumos.existsByNombreIgnoreCase(nombre)) {
            throw new ApiException(HttpStatus.CONFLICT, "El insumo «" + nombre + "» ya existe.");
        }
        Insumo guardado = insumos.save(new Insumo(nombre, solicitud.unidadMedida().trim(), BigDecimal.ZERO, solicitud.stockMinimo()));
        publicador.publishEvent(new OperacionAuditada("Inventario", "INSUMO_CREADO", usuario,
                "Insumo «" + nombre + "» registrado", nombre));
        return aDto(guardado);
    }

    @Transactional
    public InsumoDto actualizarStockMinimo(Long idInsumo, BigDecimal stockMinimo, String usuario) {
        Insumo insumo = insumos.findById(idInsumo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El insumo no existe."));
        BigDecimal anterior = insumo.getStockMinimo();
        insumo.setStockMinimo(stockMinimo.setScale(2, RoundingMode.HALF_UP));
        insumos.save(insumo);
        publicador.publishEvent(new OperacionAuditada("Inventario", "STOCK_MINIMO", usuario,
                "Stock mínimo de «" + insumo.getNombre() + "»: " + anterior + " → " + insumo.getStockMinimo(), insumo.getNombre()));
        return aDto(insumo);
    }

    /** RN-10: toda entrada registra cantidad, fecha y responsable, y aumenta el stock. */
    @Transactional
    public InsumoDto registrarEntrada(Long idInsumo, MovimientoRequest solicitud, String nombreUsuario) {
        return mover(idInsumo, TipoMovimiento.ENTRADA, solicitud, nombreUsuario);
    }

    /** RN-11 y RN-12: toda salida registra cantidad, fecha y motivo, y el stock no puede quedar en negativo. */
    @Transactional
    public InsumoDto registrarSalida(Long idInsumo, MovimientoRequest solicitud, String nombreUsuario) {
        return mover(idInsumo, TipoMovimiento.SALIDA, solicitud, nombreUsuario);
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> historial(Long idInsumo) {
        if (!insumos.existsById(idInsumo)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El insumo no existe.");
        }
        return movimientos.findTop50ByInsumoIdOrderByFechaHoraDesc(idInsumo).stream()
                .map(m -> new MovimientoDto(m.getId(), m.getTipo().name(), m.getCantidad(), m.getFechaHora(),
                        m.getMotivo(), m.getUsuario().getNombreCompleto()))
                .toList();
    }

    /** Todos los movimientos del periodo (por defecto, los últimos 30 días), con filtros opcionales. */
    @Transactional(readOnly = true)
    public List<MovimientoDetalleDto> movimientos(LocalDate desde, LocalDate hasta, TipoMovimiento tipo, Long idInsumo) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        if (fin.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la fecha inicial.");
        }
        return movimientos.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay())
                .stream()
                .filter(m -> tipo == null || m.getTipo() == tipo)
                .filter(m -> idInsumo == null || m.getInsumo().getId().equals(idInsumo))
                .sorted(Comparator.comparing(MovimientoInventario::getFechaHora).reversed())
                .limit(500)
                .map(m -> new MovimientoDetalleDto(m.getId(), m.getInsumo().getId(), m.getInsumo().getNombre(),
                        m.getInsumo().getUnidadMedida(), m.getTipo().name(), m.getCantidad(), m.getFechaHora(), m.getMotivo(),
                        m.getUsuario().getNombreCompleto()))
                .toList();
    }

    private InsumoDto mover(Long idInsumo, TipoMovimiento tipo, MovimientoRequest solicitud, String nombreUsuario) {
        Insumo insumo = insumos.buscarParaActualizar(idInsumo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El insumo no existe."));
        Usuario usuario = usuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión."));
        BigDecimal cantidad = solicitud.cantidad().setScale(2, RoundingMode.HALF_UP);
        String motivo = solicitud.motivo() == null ? "" : solicitud.motivo().trim();

        if (tipo == TipoMovimiento.SALIDA) {
            if (motivo.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Indique el motivo de la salida (consumo, merma u otro).");
            }
            if (insumo.getStockActual().compareTo(cantidad) < 0) {
                throw new ApiException(HttpStatus.CONFLICT, "No hay stock suficiente de «" + insumo.getNombre()
                        + "». Stock actual: " + insumo.getStockActual() + " " + insumo.getUnidadMedida() + ".");
            }
            insumo.disminuirStock(cantidad);
        } else {
            insumo.aumentarStock(cantidad);
            if (motivo.isEmpty()) {
                motivo = "Ingreso de mercadería";
            }
        }
        movimientos.save(new MovimientoInventario(insumo, usuario, tipo, cantidad, motivo));
        publicador.publishEvent(new OperacionAuditada("Inventario", tipo == TipoMovimiento.ENTRADA ? "ENTRADA_INSUMO" : "SALIDA_INSUMO",
                nombreUsuario, insumo.getNombre() + ": " + (tipo == TipoMovimiento.ENTRADA ? "entrada" : "salida") + " de "
                + cantidad + " " + insumo.getUnidadMedida() + " (" + motivo + ")", insumo.getNombre()));
        return aDto(insumo);
    }

    private static InsumoDto aDto(Insumo i) {
        return new InsumoDto(i.getId(), i.getNombre(), i.getUnidadMedida(), i.getStockActual(), i.getStockMinimo(),
                i.estadoStock().name());
    }
}
