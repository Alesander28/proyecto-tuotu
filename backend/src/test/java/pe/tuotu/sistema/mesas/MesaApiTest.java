package pe.tuotu.sistema.mesas;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El gerente administra las mesas y ve su ocupación calculada con los pedidos sin cobrar. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MesaApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private ApiTestSupport apoyo;

    @BeforeEach
    void preparar() {
        apoyo = new ApiTestSupport(mvc, json);
    }

    private void crearMesa(String token, String numero) throws Exception {
        mvc.perform(post("/api/mesas").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"" + numero + "\"}")).andExpect(status().isCreated());
    }

    private JsonNode mesa(String token, String numero) throws Exception {
        MvcResult r = mvc.perform(get("/api/mesas").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn();
        for (JsonNode m : apoyo.leer(r)) {
            if (numero.equals(m.get("numero").asText())) {
                return m;
            }
        }
        throw new IllegalStateException("No aparece la mesa " + numero);
    }

    @Test
    void unaMesaPasaDeLibreAOcupadaYVuelveALibreAlCobrar() throws Exception {
        String tokenGerente = apoyo.tokenGerente();
        String tokenMozo = apoyo.sesionDe("mozo_mesa1", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_mesa1", "CAJERO");
        mvc.perform(post("/api/cajas/abrir").header("Authorization", "Bearer " + tokenCajero)
                .contentType(MediaType.APPLICATION_JSON).content("{\"montoInicial\":100}")).andExpect(status().isCreated());
        crearMesa(tokenGerente, "TM1");
        assertEquals("LIBRE", mesa(tokenGerente, "TM1").get("estado").asText());

        long id = apoyo.crearPedidoAtendido(tokenMozo, "TM1");
        JsonNode ocupada = mesa(tokenGerente, "TM1");
        assertEquals("OCUPADA", ocupada.get("estado").asText());
        assertEquals("ATENDIDO", ocupada.get("pedidoEstado").asText());

        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + tokenCajero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\":" + id + ",\"metodoPago\":\"Efectivo\"}")).andExpect(status().isCreated());
        assertEquals("LIBRE", mesa(tokenGerente, "TM1").get("estado").asText());
    }

    @Test
    void noSePermiteRepetirElNumeroDeMesa() throws Exception {
        String token = apoyo.tokenGerente();
        crearMesa(token, "TM2");

        mvc.perform(post("/api/mesas").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"tm2\"}")).andExpect(status().isConflict());
    }

    @Test
    void elMozoPuedeConsultarMesasPeroNoAdministrarlas() throws Exception {
        String tokenGerente = apoyo.tokenGerente();
        String tokenMozo = apoyo.sesionDe("mozo_mesa2", "MOZO");
        crearMesa(tokenGerente, "TM3");

        mvc.perform(get("/api/mesas").header("Authorization", "Bearer " + tokenMozo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/mesas").header("Authorization", "Bearer " + tokenMozo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numero\":\"TM4\"}"))
                .andExpect(status().isForbidden());
    }
}
