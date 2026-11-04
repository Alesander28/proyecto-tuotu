package pe.tuotu.sistema.ventas.controlador;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;
import pe.tuotu.sistema.ventas.dto.VentaDto;
import pe.tuotu.sistema.ventas.dto.VentaRegistrarRequest;
import pe.tuotu.sistema.ventas.servicio.VentaService;

import java.util.List;

/** Requiere el permiso VENTAS_REGISTRAR (cajero). */
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping("/pendientes")
    public List<PedidoDto> pendientes() {
        return ventaService.pendientesDeCobro();
    }

    @GetMapping("/metodos-pago")
    public List<String> metodosPago() {
        return ventaService.metodosPago();
    }

    @GetMapping
    public List<VentaDto> recientes() {
        return ventaService.recientes();
    }

    @PostMapping
    public ResponseEntity<VentaDto> registrar(@Valid @RequestBody VentaRegistrarRequest solicitud,
                                              Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(solicitud, autenticacion.getName()));
    }
}
