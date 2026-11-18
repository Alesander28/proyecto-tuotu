package pe.tuotu.sistema.auditoria.controlador;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.auditoria.dto.RegistroAuditoriaDto;
import pe.tuotu.sistema.auditoria.servicio.AuditoriaService;

import java.time.LocalDate;
import java.util.List;

/**
 * Solo consulta (GET). Requiere AUDITORIA_VER (gerente), salvo el historial de un pedido, que requiere PEDIDOS_VER.
 * No existen rutas para crear, modificar ni borrar registros.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<RegistroAuditoriaDto> consultar(
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String usuario,
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return auditoriaService.consultar(modulo, usuario, texto, desde, hasta);
    }

    @GetMapping("/recientes")
    public List<RegistroAuditoriaDto> recientes() {
        return auditoriaService.recientes();
    }

    @GetMapping("/pedidos/{codigo}")
    public List<RegistroAuditoriaDto> historialDePedido(@PathVariable String codigo) {
        return auditoriaService.historialDePedido(codigo);
    }
}
