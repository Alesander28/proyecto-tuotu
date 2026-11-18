package pe.tuotu.sistema.reportes.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.cajas.dominio.EstadoCaja;
import pe.tuotu.sistema.cajas.repositorio.CajaRepository;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.inventario.dominio.EstadoStock;
import pe.tuotu.sistema.inventario.dominio.Insumo;
import pe.tuotu.sistema.inventario.dominio.MovimientoInventario;
import pe.tuotu.sistema.inventario.dominio.TipoMovimiento;
import pe.tuotu.sistema.inventario.repositorio.InsumoRepository;
import pe.tuotu.sistema.inventario.repositorio.MovimientoRepository;
import pe.tuotu.sistema.pedidos.dominio.DetallePedido;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.pedidos.repositorio.PedidoRepository;
import pe.tuotu.sistema.reportes.dto.AlertaStockDto;
import pe.tuotu.sistema.reportes.dto.CantidadDto;
import pe.tuotu.sistema.reportes.dto.InsumoReporteDto;
import pe.tuotu.sistema.reportes.dto.InventarioReporteDto;
import pe.tuotu.sistema.reportes.dto.MontoDto;
import pe.tuotu.sistema.reportes.dto.PedidosReporteDto;
import pe.tuotu.sistema.reportes.dto.PuntoSerieDto;
import pe.tuotu.sistema.reportes.dto.ResumenDto;
import pe.tuotu.sistema.reportes.dto.VentasReporteDto;
import pe.tuotu.sistema.ventas.dominio.Venta;
import pe.tuotu.sistema.ventas.repositorio.VentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** HU-015: reportes y panel del gerente. Solo muestra datos reales; no calcula ganancias porque no hay costos. */
@Service
public class ReporteService {

    private static final BigDecimal CERO = BigDecimal.ZERO;

    private final VentaRepository ventas;
    private final InsumoRepository insumos;
    private final MovimientoRepository movimientos;
    private final PedidoRepository pedidos;
    private final CajaRepository cajas;

    public ReporteService(VentaRepository ventas, InsumoRepository insumos, MovimientoRepository movimientos,
                          PedidoRepository pedidos, CajaRepository cajas) {
        this.ventas = ventas;
        this.insumos = insumos;
        this.movimientos = movimientos;
        this.pedidos = pedidos;
        this.cajas = cajas;
    }

    /** Cifras de hoy (y de ayer, para comparar) para el panel del gerente. */
    @Transactional(readOnly = true)
    public ResumenDto resumen() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();
        LocalDateTime inicioAyer = hoy.minusDays(1).atStartOfDay();

        List<Venta> deHoy = ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(inicioHoy, inicioManana);
        List<Venta> deAyer = ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(inicioAyer, inicioHoy);
        BigDecimal montoHoy = sumar(deHoy);
        int mesas = (int) deHoy.stream().map(v -> v.getPedido().getMesa().trim().toLowerCase(Locale.ROOT)).distinct().count();
        BigDecimal ticket = deHoy.isEmpty() ? CERO : montoHoy.divide(BigDecimal.valueOf(deHoy.size()), 2, RoundingMode.HALF_UP);

        List<Insumo> todos = insumos.findAll();
        int agotados = (int) todos.stream().filter(i -> i.estadoStock() == EstadoStock.AGOTADO).count();
        int bajos = (int) todos.stream().filter(i -> i.estadoStock() == EstadoStock.BAJO).count() + agotados;
        List<AlertaStockDto> alertas = todos.stream()
                .filter(i -> i.estadoStock() != EstadoStock.NORMAL)
                .sorted(Comparator.comparingInt((Insumo i) -> i.estadoStock().ordinal()).thenComparing(Insumo::getNombre))
                .limit(8)
                .map(i -> new AlertaStockDto(i.getNombre(), i.getUnidadMedida(), i.getStockActual(), i.getStockMinimo(), i.estadoStock().name()))
                .toList();

        return new ResumenDto(
                pedidos.countByFechaHoraGreaterThanEqualAndFechaHoraLessThan(inicioHoy, inicioManana),
                pedidos.countByFechaHoraGreaterThanEqualAndFechaHoraLessThan(inicioAyer, inicioHoy),
                deHoy.size(), montoHoy, sumar(deAyer), ticket, mesas,
                pedidos.countByEstado(EstadoPedido.PENDIENTE), pedidos.countByEstado(EstadoPedido.EN_PREPARACION),
                bajos, agotados, cajas.countByEstado(EstadoCaja.ABIERTA), alertas);
    }

    /** Si no se indican fechas, el reporte es del día de hoy. */
    @Transactional(readOnly = true)
    public VentasReporteDto ventas(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now();
        LocalDate fin = hasta != null ? hasta : inicio;
        validarRango(inicio, fin);

        List<Venta> lista = ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());

        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        Map<String, Long> unidades = new LinkedHashMap<>();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        Map<String, BigDecimal> porCajero = new LinkedHashMap<>();
        BigDecimal total = CERO;

        for (Venta venta : lista) {
            total = total.add(venta.getTotal());
            porMetodo.merge(venta.getMetodoPago(), venta.getTotal(), BigDecimal::add);
            porCajero.merge(venta.getCajero().getNombreCompleto(), venta.getTotal(), BigDecimal::add);
            for (DetallePedido d : venta.getPedido().getDetalles()) {
                String categoria = d.getPresentacion().getProducto().getCategoria().getNombre();
                String producto = d.getPresentacion().getProducto().getNombre();
                porCategoria.merge(categoria, d.calcularSubtotal(), BigDecimal::add);
                unidades.merge(producto, (long) d.getCantidad(), Long::sum);
            }
        }

        Comparator<MontoDto> mayorMonto = Comparator.comparing(MontoDto::monto).reversed();
        List<MontoDto> categorias = ordenar(porCategoria, mayorMonto);
        List<MontoDto> metodos = ordenar(porMetodo, mayorMonto);
        List<MontoDto> cajeros = ordenar(porCajero, mayorMonto);
        List<CantidadDto> masVendidos = unidades.entrySet().stream()
                .map(e -> new CantidadDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(CantidadDto::cantidad).reversed())
                .limit(5).toList();
        BigDecimal ticket = lista.isEmpty() ? CERO : total.divide(BigDecimal.valueOf(lista.size()), 2, RoundingMode.HALF_UP);

        return new VentasReporteDto(inicio, fin, lista.size(), total, categorias, masVendidos, metodos, cajeros, ticket);
    }

    /** Ventas por día, semana (desde el lunes) o mes. Los periodos sin ventas aparecen en cero. */
    @Transactional(readOnly = true)
    public List<PuntoSerieDto> serie(LocalDate desde, LocalDate hasta, String agrupar) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        validarRango(inicio, fin);
        if (inicio.plusDays(732).isBefore(fin)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El rango no puede superar los dos años.");
        }
        String modo = agrupar == null ? "dia" : agrupar.trim().toLowerCase(Locale.ROOT);
        if (!List.of("dia", "semana", "mes").contains(modo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Agrupe por «dia», «semana» o «mes».");
        }

        Map<LocalDate, Integer> cantidades = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> montos = new LinkedHashMap<>();
        LocalDate cursor = inicioDe(inicio, modo);
        LocalDate ultimo = inicioDe(fin, modo);
        while (!cursor.isAfter(ultimo)) {
            cantidades.put(cursor, 0);
            montos.put(cursor, CERO);
            cursor = switch (modo) {
                case "semana" -> cursor.plusWeeks(1);
                case "mes" -> cursor.plusMonths(1);
                default -> cursor.plusDays(1);
            };
        }
        for (Venta v : ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay())) {
            LocalDate clave = inicioDe(v.getFechaHora().toLocalDate(), modo);
            cantidades.merge(clave, 1, Integer::sum);
            montos.merge(clave, v.getTotal(), BigDecimal::add);
        }
        List<PuntoSerieDto> resultado = new ArrayList<>();
        cantidades.forEach((periodo, n) -> resultado.add(new PuntoSerieDto(periodo, n, montos.get(periodo))));
        return resultado;
    }

    /** Pedidos del periodo por estado y por mozo. */
    @Transactional(readOnly = true)
    public PedidosReporteDto pedidos(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now();
        LocalDate fin = hasta != null ? hasta : inicio;
        validarRango(inicio, fin);
        List<Pedido> lista = pedidos.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
                inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());

        List<CantidadDto> porEstado = new ArrayList<>();
        for (EstadoPedido estado : EstadoPedido.values()) {
            porEstado.add(new CantidadDto(estado.getTexto(), lista.stream().filter(p -> p.getEstado() == estado).count()));
        }
        Map<String, Long> porMozo = new LinkedHashMap<>();
        for (Pedido p : lista) {
            porMozo.merge(p.getMozo().getNombreCompleto(), 1L, Long::sum);
        }
        List<CantidadDto> mozos = porMozo.entrySet().stream().map(e -> new CantidadDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(CantidadDto::cantidad).reversed()).toList();
        return new PedidosReporteDto(inicio, fin, lista.size(), porEstado, mozos);
    }

    /** Stock actual de cada insumo y lo que entró y salió en el periodo. */
    @Transactional(readOnly = true)
    public InventarioReporteDto inventario(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now();
        LocalDate fin = hasta != null ? hasta : inicio;
        validarRango(inicio, fin);

        Map<Long, BigDecimal> entradas = new HashMap<>();
        Map<Long, BigDecimal> salidas = new HashMap<>();
        for (MovimientoInventario m : movimientos.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay())) {
            Map<Long, BigDecimal> destino = m.getTipo() == TipoMovimiento.ENTRADA ? entradas : salidas;
            destino.merge(m.getInsumo().getId(), m.getCantidad(), BigDecimal::add);
        }

        List<InsumoReporteDto> filas = insumos.findAll().stream()
                .sorted(Comparator.comparing(Insumo::getNombre))
                .map(i -> new InsumoReporteDto(i.getNombre(), i.getUnidadMedida(), i.getStockActual(),
                        entradas.getOrDefault(i.getId(), CERO), salidas.getOrDefault(i.getId(), CERO)))
                .toList();
        return new InventarioReporteDto(inicio, fin, filas);
    }

    private static LocalDate inicioDe(LocalDate fecha, String modo) {
        return switch (modo) {
            case "semana" -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes" -> fecha.withDayOfMonth(1);
            default -> fecha;
        };
    }

    private static BigDecimal sumar(List<Venta> lista) {
        return lista.stream().map(Venta::getTotal).reduce(CERO, BigDecimal::add);
    }

    private static List<MontoDto> ordenar(Map<String, BigDecimal> mapa, Comparator<MontoDto> orden) {
        return mapa.entrySet().stream().map(e -> new MontoDto(e.getKey(), e.getValue())).sorted(orden).toList();
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la fecha inicial.");
        }
    }
}
