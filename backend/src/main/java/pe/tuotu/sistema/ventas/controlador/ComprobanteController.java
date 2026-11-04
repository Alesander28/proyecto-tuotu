package pe.tuotu.sistema.ventas.controlador;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.ventas.dominio.Comprobante;
import pe.tuotu.sistema.ventas.servicio.ComprobanteService;

/** Requiere el permiso VENTAS_REGISTRAR (cajero). */
@RestController
@RequestMapping("/api/comprobantes")
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    public ComprobanteController(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Comprobante comprobante = comprobanteService.obtener(id);
        byte[] contenido = comprobanteService.generarPdf(comprobante);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + comprobante.getNumero() + ".pdf\"")
                .body(contenido);
    }
}
