package pe.tuotu.sistema.ventas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.tuotu.sistema.comun.ApiTestSupport;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la API de ventas y comprobantes (HU-009 y HU-014). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class VentaApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private ApiTestSupport apoyo;

    @BeforeEach
    void preparar() {
        apoyo = new ApiTestSupport(mvc, json);
    }

    private MvcResult cobrar(String token, long pedidoId, String metodo) throws Exception {
        return mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\":" + pedidoId + ",\"metodoPago\":\"" + metodo + "\"}")).andReturn();
    }

    @Test
    void elCajeroCobraUnPedidoAtendidoYRecibeSuBoleta() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_ven1", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_ven1", "CAJERO");
        long pedidoId = apoyo.crearPedidoAtendido(tokenMozo);

        MvcResult pendientes = mvc.perform(get("/api/ventas/pendientes").header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isOk()).andReturn();
        boolean aparece = false;
        for (JsonNode p : apoyo.leer(pendientes)) {
            aparece |= p.get("id").asLong() == pedidoId;
        }
        assertTrue(aparece, "el pedido atendido debe aparecer como pendiente de cobro");

        MvcResult venta = cobrar(tokenCajero, pedidoId, "Efectivo");

        assertEquals(201, venta.getResponse().getStatus());
        JsonNode datos = apoyo.leer(venta);
        assertTrue(datos.get("comprobanteNumero").asText().startsWith("B001-"));
        assertEquals("Efectivo", datos.get("metodoPago").asText());
    }

    @Test
    void unPedidoNoSeCobraDosVeces() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_ven2", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_ven2", "CAJERO");
        long pedidoId = apoyo.crearPedidoAtendido(tokenMozo);

        assertEquals(201, cobrar(tokenCajero, pedidoId, "Yape").getResponse().getStatus());
        assertEquals(409, cobrar(tokenCajero, pedidoId, "Yape").getResponse().getStatus());
    }

    @Test
    void noSeCobraUnPedidoQueAunNoFueAtendido() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_ven3", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_ven3", "CAJERO");
        long pedidoId = apoyo.crearPedido(tokenMozo);

        assertEquals(409, cobrar(tokenCajero, pedidoId, "Efectivo").getResponse().getStatus());
    }

    @Test
    void unMozoNoPuedeCobrar() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_ven4", "MOZO");
        long pedidoId = apoyo.crearPedidoAtendido(tokenMozo);

        assertEquals(403, cobrar(tokenMozo, pedidoId, "Efectivo").getResponse().getStatus());
    }

    @Test
    void lasBoletasTienenNumeracionDistintaYSePuedenDescargarEnPdf() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_ven5", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_ven5", "CAJERO");
        JsonNode primera = apoyo.leer(cobrar(tokenCajero, apoyo.crearPedidoAtendido(tokenMozo), "Efectivo"));
        JsonNode segunda = apoyo.leer(cobrar(tokenCajero, apoyo.crearPedidoAtendido(tokenMozo), "Efectivo"));

        assertNotEquals(primera.get("comprobanteNumero").asText(), segunda.get("comprobanteNumero").asText());

        MvcResult pdf = mvc.perform(get("/api/comprobantes/" + primera.get("comprobanteId").asLong() + "/pdf")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isOk()).andReturn();
        assertEquals("application/pdf", pdf.getResponse().getContentType());
        String inicio = new String(pdf.getResponse().getContentAsByteArray(), 0, 4, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF", inicio);
    }
}
