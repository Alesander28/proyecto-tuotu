package pe.tuotu.sistema.reportes.controlador;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.reportes.dto.InventarioReporteDto;
import pe.tuotu.sistema.reportes.dto.PedidosReporteDto;
import pe.tuotu.sistema.reportes.dto.PuntoSerieDto;
import pe.tuotu.sistema.reportes.dto.ResumenDto;
import pe.tuotu.sistema.reportes.dto.VentasReporteDto;
import pe.tuotu.sistema.reportes.servicio.ReporteService;

import java.time.LocalDate;
import java.util.List;

/** Requiere el permiso REPORTES_VER (gerente). Las fechas van como aaaa-mm-dd; sin fechas, el reporte es de hoy. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Cifras de hoy para el panel del gerente. */
    @GetMapping("/resumen")
    public ResumenDto resumen() {
        return reporteService.resumen();
    }

    /** Serie de ventas agrupada por «dia», «semana» o «mes». */
    @GetMapping("/serie")
    public List<PuntoSerieDto> serie(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam(defaultValue = "dia") String agrupar) {
        return reporteService.serie(desde, hasta, agrupar);
    }

    @GetMapping("/pedidos")
    public PedidosReporteDto pedidos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.pedidos(desde, hasta);
    }

    @GetMapping("/ventas")
    public VentasReporteDto ventas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.ventas(desde, hasta);
    }

    @GetMapping("/inventario")
    public InventarioReporteDto inventario(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.inventario(desde, hasta);
    }
}
