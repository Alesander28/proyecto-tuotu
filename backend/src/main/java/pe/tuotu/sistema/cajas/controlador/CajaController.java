package pe.tuotu.sistema.cajas.controlador;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.cajas.dominio.EstadoCaja;
import pe.tuotu.sistema.cajas.dto.AbrirCajaRequest;
import pe.tuotu.sistema.cajas.dto.CajaDto;
import pe.tuotu.sistema.cajas.dto.CerrarCajaRequest;
import pe.tuotu.sistema.cajas.servicio.CajaService;

import java.time.LocalDate;
import java.util.List;

/**
 * El cajero abre y cierra su propia caja (CAJAS_OPERAR: /actual, /abrir y /cerrar). El gerente consulta todas las
 * cajas (CAJAS_VER: GET /api/cajas).
 */
@RestController
@RequestMapping("/api/cajas")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping("/actual")
    public ResponseEntity<CajaDto> actual(Authentication autenticacion) {
        return cajaService.actual(autenticacion.getName()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/abrir")
    public ResponseEntity<CajaDto> abrir(@Valid @RequestBody AbrirCajaRequest solicitud, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.abrir(solicitud.montoInicial(), autenticacion.getName()));
    }

    @PostMapping("/cerrar")
    public CajaDto cerrar(@Valid @RequestBody CerrarCajaRequest solicitud, Authentication autenticacion) {
        return cajaService.cerrar(solicitud.montoContado(), solicitud.observacion(), autenticacion.getName());
    }

    @GetMapping
    public List<CajaDto> listar(
            @RequestParam(required = false) EstadoCaja estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return cajaService.listar(estado, desde, hasta);
    }
}
