package pe.tuotu.sistema.inventario.controlador;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.inventario.dto.InsumoCrearRequest;
import pe.tuotu.sistema.inventario.dto.InsumoDto;
import pe.tuotu.sistema.inventario.dto.MovimientoDto;
import pe.tuotu.sistema.inventario.dto.MovimientoRequest;
import pe.tuotu.sistema.inventario.servicio.InsumoService;

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
    public ResponseEntity<InsumoDto> crear(@Valid @RequestBody InsumoCrearRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insumoService.crear(solicitud));
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
}
