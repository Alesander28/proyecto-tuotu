package pe.tuotu.sistema.mesas.controlador;

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
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.mesas.dto.MesaActivaRequest;
import pe.tuotu.sistema.mesas.dto.MesaCrearRequest;
import pe.tuotu.sistema.mesas.dto.MesaDto;
import pe.tuotu.sistema.mesas.servicio.MesaService;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;

import java.util.List;

/** Requiere el permiso MESAS_GESTIONAR (gerente). */
@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    public List<MesaDto> listar() {
        return mesaService.listar();
    }

    @PostMapping
    public ResponseEntity<MesaDto> crear(@Valid @RequestBody MesaCrearRequest solicitud, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mesaService.crear(solicitud.numero(), autenticacion.getName()));
    }

    @PatchMapping("/{id}/activa")
    public ResponseEntity<Void> activar(@PathVariable Long id, @RequestBody MesaActivaRequest solicitud, Authentication autenticacion) {
        mesaService.cambiarActiva(id, solicitud.activa(), autenticacion.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historial")
    public List<PedidoDto> historial(@PathVariable Long id) {
        return mesaService.historial(id);
    }
}
