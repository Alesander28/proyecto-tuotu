package pe.tuotu.sistema.reportes.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.inventario.dominio.Insumo;
import pe.tuotu.sistema.inventario.dominio.MovimientoInventario;
import pe.tuotu.sistema.inventario.dominio.TipoMovimiento;
import pe.tuotu.sistema.inventario.repositorio.InsumoRepository;
import pe.tuotu.sistema.inventario.repositorio.MovimientoRepository;
import pe.tuotu.sistema.pedidos.dominio.DetallePedido;
import pe.tuotu.sistema.reportes.dto.CantidadDto;
import pe.tuotu.sistema.reportes.dto.InsumoReporteDto;
import pe.tuotu.sistema.reportes.dto.InventarioReporteDto;
import pe.tuotu.sistema.reportes.dto.MontoDto;
import pe.tuotu.sistema.reportes.dto.VentasReporteDto;
import pe.tuotu.sistema.ventas.dominio.Venta;
import pe.tuotu.sistema.ventas.repositorio.VentaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HU-015: reportes de ventas y de inventario para el gerente. */
@Service
public class ReporteService {

    private final VentaRepository ventas;
    private final InsumoRepository insumos;
    private final MovimientoRepository movimientos;

    public ReporteService(VentaRepository ventas, InsumoRepository insumos, MovimientoRepository movimientos) {
        this.ventas = ventas;
        this.insumos = insumos;
        this.movimientos = movimientos;
    }

    /** Si no se indican fechas, el reporte es del día de hoy. */
    @Transactional(readOnly = true)
    public VentasReporteDto ventas(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now();
        LocalDate fin = hasta != null ? hasta : inicio;
        validarRango(inicio, fin);

        List<Venta> lista = ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());

        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        Map<String, Long> unidades = new LinkedHashMap<>();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Venta venta : lista) {
            total = total.add(venta.getTotal());
            porMetodo.merge(venta.getMetodoPago(), venta.getTotal(), BigDecimal::add);
            for (DetallePedido d : venta.getPedido().getDetalles()) {
                String categoria = d.getPresentacion().getProducto().getCategoria().getNombre();
                String producto = d.getPresentacion().getProducto().getNombre();
                porCategoria.merge(categoria, d.calcularSubtotal(), BigDecimal::add);
                unidades.merge(producto, (long) d.getCantidad(), Long::sum);
            }
        }

        Comparator<MontoDto> mayorMonto = Comparator.comparing(MontoDto::monto).reversed();
        List<MontoDto> categorias = porCategoria.entrySet().stream()
                .map(e -> new MontoDto(e.getKey(), e.getValue())).sorted(mayorMonto).toList();
        List<MontoDto> metodos = porMetodo.entrySet().stream()
                .map(e -> new MontoDto(e.getKey(), e.getValue())).sorted(mayorMonto).toList();
        List<CantidadDto> masVendidos = unidades.entrySet().stream()
                .map(e -> new CantidadDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(CantidadDto::cantidad).reversed())
                .limit(5).toList();

        return new VentasReporteDto(inicio, fin, lista.size(), total, categorias, masVendidos, metodos);
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
                        entradas.getOrDefault(i.getId(), BigDecimal.ZERO), salidas.getOrDefault(i.getId(), BigDecimal.ZERO)))
                .toList();
        return new InventarioReporteDto(inicio, fin, filas);
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la fecha inicial.");
        }
    }
}
