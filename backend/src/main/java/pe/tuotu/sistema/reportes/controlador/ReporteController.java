package pe.tuotu.sistema.reportes.controlador;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.reportes.dto.InventarioReporteDto;
import pe.tuotu.sistema.reportes.dto.VentasReporteDto;
import pe.tuotu.sistema.reportes.servicio.ReporteService;

import java.time.LocalDate;

/** Requiere el permiso REPORTES_VER (gerente). Las fechas van como aaaa-mm-dd; sin fechas, el reporte es de hoy. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
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
