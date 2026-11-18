package pe.tuotu.sistema.auditoria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.tuotu.sistema.comun.ApiTestSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La auditoría registra las operaciones, solo el gerente la consulta y nadie puede modificarla ni borrarla. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuditoriaApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private ApiTestSupport apoyo;

    @BeforeEach
    void preparar() {
        apoyo = new ApiTestSupport(mvc, json);
    }

    private boolean contiene(JsonNode lista, String tipo, String usuario) {
        for (JsonNode r : lista) {
            if (tipo.equals(r.get("tipo").asText()) && usuario.equals(r.get("usuario").asText())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void lasOperacionesQuedanRegistradasYElGerenteLasConsulta() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_audi1", "MOZO");
        long id = apoyo.crearPedido(tokenMozo);
        apoyo.cambiarEstado(tokenMozo, id, "EN_PREPARACION");

        MvcResult resultado = mvc.perform(get("/api/auditoria").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isOk()).andReturn();
        JsonNode lista = apoyo.leer(resultado);

        assertTrue(contiene(lista, "PEDIDO_REGISTRADO", "mozo_audi1"));
        assertTrue(contiene(lista, "CAMBIO_ESTADO_PEDIDO", "mozo_audi1"));
        assertTrue(contiene(lista, "USUARIO_CREADO", "gerente"));
        assertTrue(contiene(lista, "INICIO_SESION", "mozo_audi1"));
    }

    @Test
    void elHistorialDeUnPedidoMuestraSusCambiosDeEstado() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_audi2", "MOZO");
        long id = apoyo.crearPedido(tokenMozo);
        apoyo.cambiarEstado(tokenMozo, id, "EN_PREPARACION");
        String codigo = String.format("%04d", id);

        MvcResult resultado = mvc.perform(get("/api/auditoria/pedidos/" + codigo).header("Authorization", "Bearer " + tokenMozo))
                .andExpect(status().isOk()).andReturn();

        assertTrue(apoyo.leer(resultado).size() >= 2);
    }

    @Test
    void soloElGerenteConsultaLaAuditoriaGeneral() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_audi3", "MOZO");

        mvc.perform(get("/api/auditoria").header("Authorization", "Bearer " + tokenMozo)).andExpect(status().isForbidden());
    }

    @Test
    void losRegistrosNoSePuedenModificarNiBorrar() throws Exception {
        String tokenGerente = apoyo.tokenGerente();

        mvc.perform(delete("/api/auditoria/1").header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().is4xxClientError());
        mvc.perform(put("/api/auditoria/1").header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().is4xxClientError());
    }
}
