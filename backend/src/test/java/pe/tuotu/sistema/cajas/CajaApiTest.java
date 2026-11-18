package pe.tuotu.sistema.cajas;

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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Apertura y cierre de caja con arqueo, y su consulta por el gerente. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CajaApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private ApiTestSupport apoyo;

    @BeforeEach
    void preparar() {
        apoyo = new ApiTestSupport(mvc, json);
    }

    private BigDecimal numero(JsonNode nodo, String campo) {
        return new BigDecimal(nodo.get(campo).asText());
    }

    @Test
    void elCierreComparaLoContadoConLoEsperado() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_caja1", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_caja1", "CAJERO");

        mvc.perform(post("/api/cajas/abrir").header("Authorization", "Bearer " + tokenCajero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoInicial\":100}")).andExpect(status().isCreated());
        // una segunda apertura se rechaza
        mvc.perform(post("/api/cajas/abrir").header("Authorization", "Bearer " + tokenCajero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoInicial\":50}")).andExpect(status().isConflict());

        long pedidoId = apoyo.crearPedidoAtendido(tokenMozo);
        MvcResult venta = mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":" + pedidoId + ",\"metodoPago\":\"Efectivo\"}"))
                .andExpect(status().isCreated()).andReturn();
        BigDecimal total = numero(apoyo.leer(venta), "total");

        MvcResult cierre = mvc.perform(post("/api/cajas/cerrar").header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoContado\":" + new BigDecimal("100").add(total) + ",\"observacion\":\"Todo conforme\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode caja = apoyo.leer(cierre);

        assertEquals("CERRADA", caja.get("estado").asText());
        assertEquals(0, new BigDecimal("100").add(total).compareTo(numero(caja, "efectivoEsperado")));
        assertEquals(0, BigDecimal.ZERO.compareTo(numero(caja, "diferencia")));
        assertEquals(1, caja.get("cantidadVentas").asInt());
    }

    @Test
    void unaCajaCerradaConFaltanteMuestraLaDiferencia() throws Exception {
        String tokenCajero = apoyo.sesionDe("cajero_caja2", "CAJERO");
        mvc.perform(post("/api/cajas/abrir").header("Authorization", "Bearer " + tokenCajero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoInicial\":100}")).andExpect(status().isCreated());

        MvcResult cierre = mvc.perform(post("/api/cajas/cerrar").header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montoContado\":90}"))
                .andExpect(status().isOk()).andReturn();

        assertEquals(0, new BigDecimal("-10").compareTo(numero(apoyo.leer(cierre), "diferencia")));
    }

    @Test
    void elGerenteConsultaLasCajasYElCajeroNo() throws Exception {
        String tokenCajero = apoyo.sesionDe("cajero_caja3", "CAJERO");
        mvc.perform(post("/api/cajas/abrir").header("Authorization", "Bearer " + tokenCajero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoInicial\":20}")).andExpect(status().isCreated());

        MvcResult lista = mvc.perform(get("/api/cajas").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isOk()).andReturn();
        boolean aparece = false;
        for (JsonNode c : apoyo.leer(lista)) {
            aparece |= "cajero_caja3".equals(c.get("cajeroUsuario").asText()) && "ABIERTA".equals(c.get("estado").asText());
        }
        assertTrue(aparece);

        mvc.perform(get("/api/cajas").header("Authorization", "Bearer " + tokenCajero)).andExpect(status().isForbidden());
    }

    @Test
    void elGerenteNoOperaCajas() throws Exception {
        mvc.perform(post("/api/cajas/abrir").header("Authorization", "Bearer " + apoyo.tokenGerente())
                .contentType(MediaType.APPLICATION_JSON).content("{\"montoInicial\":10}")).andExpect(status().isForbidden());
    }
}
