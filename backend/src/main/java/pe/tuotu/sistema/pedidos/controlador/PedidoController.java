package pe.tuotu.sistema.pedidos.controlador;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dto.CambiarEstadoRequest;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;
import pe.tuotu.sistema.pedidos.dto.PedidoRegistrarRequest;
import pe.tuotu.sistema.pedidos.servicio.PedidoService;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /** Requiere el permiso PEDIDOS_REGISTRAR (mozo). */
    @PostMapping
    public ResponseEntity<PedidoDto> registrar(@Valid @RequestBody PedidoRegistrarRequest solicitud,
                                               Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.registrar(solicitud, autenticacion.getName()));
    }

    /** Requiere PEDIDOS_VER. Con ?estado=PENDIENTE (u otro) filtra por estado. */
    @GetMapping
    public List<PedidoDto> listar(@RequestParam(required = false) EstadoPedido estado) {
        return pedidoService.listar(estado);
    }

    @GetMapping("/{id}")
    public PedidoDto obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    /** Requiere PEDIDOS_ESTADO. */
    @PatchMapping("/{id}/estado")
    public PedidoDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest solicitud) {
        return pedidoService.cambiarEstado(id, solicitud.estado());
    }
}
