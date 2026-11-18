package pe.tuotu.sistema.inventario.controlador;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.inventario.dominio.TipoMovimiento;
import pe.tuotu.sistema.inventario.dto.InsumoCrearRequest;
import pe.tuotu.sistema.inventario.dto.InsumoDto;
import pe.tuotu.sistema.inventario.dto.MovimientoDetalleDto;
import pe.tuotu.sistema.inventario.dto.MovimientoDto;
import pe.tuotu.sistema.inventario.dto.MovimientoRequest;
import pe.tuotu.sistema.inventario.dto.StockMinimoRequest;
import pe.tuotu.sistema.inventario.servicio.InsumoService;

import java.time.LocalDate;
import java.util.List;

/** Solo accesible con el permiso INVENTARIO_GESTIONAR (gerente). */
@RestController
@RequestMapping("/api/insumos")
public class InsumoController {

    private final InsumoService insumoService;

    public InsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    @GetMapping
    public List<InsumoDto> listar(@RequestParam(name = "q", required = false) String texto) {
        return insumoService.listar(texto);
    }

    @PostMapping
    public ResponseEntity<InsumoDto> crear(@Valid @RequestBody InsumoCrearRequest solicitud, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insumoService.crear(solicitud, autenticacion.getName()));
    }

    @PutMapping("/{id}/stock-minimo")
    public InsumoDto stockMinimo(@PathVariable Long id, @Valid @RequestBody StockMinimoRequest solicitud,
                                 Authentication autenticacion) {
        return insumoService.actualizarStockMinimo(id, solicitud.stockMinimo(), autenticacion.getName());
    }

    @PostMapping("/{id}/entradas")
    public InsumoDto entrada(@PathVariable Long id, @Valid @RequestBody MovimientoRequest solicitud,
                             Authentication autenticacion) {
        return insumoService.registrarEntrada(id, solicitud, autenticacion.getName());
    }

    @PostMapping("/{id}/salidas")
    public InsumoDto salida(@PathVariable Long id, @Valid @RequestBody MovimientoRequest solicitud,
                            Authentication autenticacion) {
        return insumoService.registrarSalida(id, solicitud, autenticacion.getName());
    }

    @GetMapping("/{id}/movimientos")
    public List<MovimientoDto> historial(@PathVariable Long id) {
        return insumoService.historial(id);
    }

    /** Movimientos de todos los insumos, con filtros por fechas, tipo e insumo. */
    @GetMapping("/movimientos")
    public List<MovimientoDetalleDto> movimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) Long insumoId) {
        return insumoService.movimientos(desde, hasta, tipo, insumoId);
    }
}
