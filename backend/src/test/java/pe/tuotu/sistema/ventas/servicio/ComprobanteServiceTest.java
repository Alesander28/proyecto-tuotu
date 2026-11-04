package pe.tuotu.sistema.ventas.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.ventas.dominio.Comprobante;
import pe.tuotu.sistema.ventas.repositorio.ComprobanteRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** Comprueba que el servicio solo depende del contrato GeneradorComprobante (Adapter), no de la biblioteca de PDF. */
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    @Mock
    private ComprobanteRepository comprobantes;

    @Test
    void entregaElDocumentoQueProduceElGeneradorSinDependerDeUnaBibliotecaDePdf() {
        byte[] documento = "documento de prueba".getBytes(StandardCharsets.UTF_8);
        ComprobanteService servicio = new ComprobanteService(comprobantes, comprobante -> documento);

        assertArrayEquals(documento, servicio.generarPdf(new Comprobante(null)));
    }

    @Test
    void siElComprobanteNoExisteResponde404() {
        when(comprobantes.findById(9L)).thenReturn(Optional.empty());
        ComprobanteService servicio = new ComprobanteService(comprobantes, comprobante -> new byte[0]);

        ApiException error = assertThrows(ApiException.class, () -> servicio.obtener(9L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
    }
}
