package pe.tuotu.sistema.ventas.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.ventas.dominio.Comprobante;
import pe.tuotu.sistema.ventas.repositorio.ComprobanteRepository;

/**
 * HU-014: entrega el comprobante de una venta. El documento lo produce un {@link GeneradorComprobante}
 * (hoy, el adaptador de OpenPDF), por lo que este servicio no depende de ninguna biblioteca de PDF.
 */
@Service
public class ComprobanteService {

    private final ComprobanteRepository comprobantes;
    private final GeneradorComprobante generador;

    public ComprobanteService(ComprobanteRepository comprobantes, GeneradorComprobante generador) {
        this.comprobantes = comprobantes;
        this.generador = generador;
    }

    @Transactional(readOnly = true)
    public Comprobante obtener(Long id) {
        return comprobantes.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El comprobante no existe."));
    }

    public byte[] generarPdf(Comprobante comprobante) {
        return generador.generar(comprobante);
    }
}
