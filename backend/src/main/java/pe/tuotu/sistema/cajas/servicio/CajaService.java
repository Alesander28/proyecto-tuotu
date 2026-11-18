package pe.tuotu.sistema.cajas.servicio;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.auditoria.evento.OperacionAuditada;
import pe.tuotu.sistema.cajas.dominio.Caja;
import pe.tuotu.sistema.cajas.dominio.EstadoCaja;
import pe.tuotu.sistema.cajas.dto.CajaDto;
import pe.tuotu.sistema.cajas.repositorio.CajaRepository;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import pe.tuotu.sistema.ventas.dominio.Venta;
import pe.tuotu.sistema.ventas.repositorio.VentaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Apertura y cierre de caja. El cajero abre su turno con un monto inicial y lo cierra contando el efectivo. El
 * sistema compara lo contado con lo esperado (monto inicial más ventas en efectivo) y guarda la diferencia.
 */
@Service
public class CajaService {

    private final CajaRepository cajas;
    private final VentaRepository ventas;
    private final UsuarioRepository usuarios;
    private final ApplicationEventPublisher publicador;

    public CajaService(CajaRepository cajas, VentaRepository ventas, UsuarioRepository usuarios,
                       ApplicationEventPublisher publicador) {
        this.cajas = cajas;
        this.ventas = ventas;
        this.usuarios = usuarios;
        this.publicador = publicador;
    }

    @Transactional
    public CajaDto abrir(BigDecimal montoInicial, String nombreUsuario) {
        Usuario cajero = cajero(nombreUsuario);
        if (cajas.findFirstByCajeroIdAndEstado(cajero.getId(), EstadoCaja.ABIERTA).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya tiene una caja abierta. Ciérrela antes de abrir otra.");
        }
        Caja caja = cajas.save(new Caja(cajero, montoInicial));
        publicador.publishEvent(new OperacionAuditada("Cajas", "CAJA_ABIERTA", nombreUsuario,
                "Caja abierta con S/ " + montoInicial, String.valueOf(caja.getId())));
        return aDto(caja);
    }

    @Transactional(readOnly = true)
    public Optional<CajaDto> actual(String nombreUsuario) {
        Usuario cajero = cajero(nombreUsuario);
        return cajas.findFirstByCajeroIdAndEstado(cajero.getId(), EstadoCaja.ABIERTA).map(this::aDto);
    }

    @Transactional
    public CajaDto cerrar(BigDecimal montoContado, String observacion, String nombreUsuario) {
        Usuario cajero = cajero(nombreUsuario);
        Caja caja = cajas.findFirstByCajeroIdAndEstado(cajero.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tiene una caja abierta."));
        BigDecimal esperado = caja.getMontoInicial().add(sumaEfectivo(turno(caja)));
        caja.cerrar(montoContado, esperado, observacion == null || observacion.isBlank() ? null : observacion.trim());
        cajas.save(caja);
        publicador.publishEvent(new OperacionAuditada("Cajas", "CAJA_CERRADA", nombreUsuario,
                "Caja cerrada: esperado S/ " + esperado + ", contado S/ " + montoContado + ", diferencia S/ " + caja.getDiferencia(),
                String.valueOf(caja.getId())));
        return aDto(caja);
    }

    /** Consulta del gerente. Sin fechas, los últimos 30 días de aperturas. */
    @Transactional(readOnly = true)
    public List<CajaDto> listar(EstadoCaja estado, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        if (fin.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la fecha inicial.");
        }
        return cajas.findByFechaAperturaGreaterThanEqualAndFechaAperturaLessThanOrderByFechaAperturaDesc(
                        inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()).stream()
                .filter(c -> estado == null || c.getEstado() == estado)
                .map(this::aDto).toList();
    }

    private Usuario cajero(String nombreUsuario) {
        return usuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión."));
    }

    private List<Venta> turno(Caja caja) {
        LocalDateTime fin = caja.getFechaCierre() != null ? caja.getFechaCierre() : LocalDateTime.now().plusSeconds(1);
        return ventas.findByCajeroIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                caja.getCajero().getId(), caja.getFechaApertura(), fin.plusSeconds(1));
    }

    private static BigDecimal sumaEfectivo(List<Venta> lista) {
        return lista.stream().filter(v -> "Efectivo".equalsIgnoreCase(v.getMetodoPago()))
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CajaDto aDto(Caja c) {
        List<Venta> lista = turno(c);
        BigDecimal total = lista.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivo = sumaEfectivo(lista);
        BigDecimal esperado = c.getEfectivoEsperado() != null ? c.getEfectivoEsperado() : c.getMontoInicial().add(efectivo);
        return new CajaDto(c.getId(), c.getCajero().getNombreCompleto(), c.getCajero().getNombreUsuario(), c.getFechaApertura(),
                c.getMontoInicial(), c.getFechaCierre(), c.getEstado().name(), lista.size(), total, efectivo, esperado,
                c.getMontoContado(), c.getDiferencia(), c.getObservacion());
    }
}
