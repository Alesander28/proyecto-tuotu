package pe.tuotu.sistema.inventario;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la API de inventario y de administración de la carta (HU-010 a HU-013). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InventarioApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private ApiTestSupport apoyo;

    @BeforeEach
    void preparar() {
        apoyo = new ApiTestSupport(mvc, json);
    }

    private long crearInsumo(String token, String nombre) throws Exception {
        MvcResult r = mvc.perform(post("/api/insumos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"unidadMedida\":\"kg\"}"))
                .andExpect(status().isCreated()).andReturn();
        return apoyo.leer(r).get("id").asLong();
    }

    private MvcResult mover(String token, long id, String tipo, String cantidad, String motivo) throws Exception {
        return mvc.perform(post("/api/insumos/" + id + "/" + tipo).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":" + cantidad + ",\"motivo\":\"" + motivo + "\"}")).andReturn();
    }

    @Test
    void entradasYSalidasActualizanElStock() throws Exception {
        String token = apoyo.tokenGerente();
        long id = crearInsumo(token, "Harina prueba 1");

        MvcResult entrada = mover(token, id, "entradas", "10", "");
        assertEquals(200, entrada.getResponse().getStatus());
        assertEquals(0, new BigDecimal("10.00").compareTo(new BigDecimal(apoyo.leer(entrada).get("stockActual").asText())));

        MvcResult salida = mover(token, id, "salidas", "4", "Consumo");
        assertEquals(200, salida.getResponse().getStatus());
        assertEquals(0, new BigDecimal("6.00").compareTo(new BigDecimal(apoyo.leer(salida).get("stockActual").asText())));

        MvcResult historial = mvc.perform(get("/api/insumos/" + id + "/movimientos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertEquals(2, apoyo.leer(historial).size());
    }

    @Test
    void unaSalidaMayorAlStockSeRechazaYNoCambiaElStock() throws Exception {
        String token = apoyo.tokenGerente();
        long id = crearInsumo(token, "Harina prueba 2");
        mover(token, id, "entradas", "5", "");

        MvcResult salida = mover(token, id, "salidas", "8", "Consumo");

        assertEquals(409, salida.getResponse().getStatus());
        MvcResult lista = mvc.perform(get("/api/insumos").param("q", "Harina prueba 2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertEquals(0, new BigDecimal("5.00").compareTo(new BigDecimal(apoyo.leer(lista).get(0).get("stockActual").asText())));
    }

    @Test
    void unMozoNoPuedeUsarElInventario() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_inv1", "MOZO");

        mvc.perform(get("/api/insumos").header("Authorization", "Bearer " + tokenMozo)).andExpect(status().isForbidden());
        mvc.perform(get("/api/productos").header("Authorization", "Bearer " + tokenMozo)).andExpect(status().isForbidden());
    }

    @Test
    void unProductoNuevoApareceEnLaCartaYAlDesactivarloDesaparece() throws Exception {
        String tokenGerente = apoyo.tokenGerente();
        String tokenMozo = apoyo.sesionDe("mozo_inv2", "MOZO");

        MvcResult categorias = mvc.perform(get("/api/productos/categorias").header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().isOk()).andReturn();
        long categoriaId = apoyo.leer(categorias).get(0).get("id").asLong();

        MvcResult creado = mvc.perform(post("/api/productos").header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pizza de prueba\",\"categoriaId\":" + categoriaId
                                + ",\"presentaciones\":[{\"nombre\":\"Personal\",\"precio\":20.00}]}"))
                .andExpect(status().isCreated()).andReturn();
        long productoId = apoyo.leer(creado).get("id").asLong();

        assertTrue(cartaContiene(tokenMozo, "Pizza de prueba"));

        mvc.perform(put("/api/productos/" + productoId).header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pizza de prueba\",\"categoriaId\":" + categoriaId + ",\"activo\":false}"))
                .andExpect(status().isOk());

        assertFalse(cartaContiene(tokenMozo, "Pizza de prueba"));
    }

    @Test
    void unProductoSinPresentacionesSeRechaza() throws Exception {
        String tokenGerente = apoyo.tokenGerente();
        MvcResult categorias = mvc.perform(get("/api/productos/categorias").header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().isOk()).andReturn();
        long categoriaId = apoyo.leer(categorias).get(0).get("id").asLong();

        mvc.perform(post("/api/productos").header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sin precio\",\"categoriaId\":" + categoriaId + ",\"presentaciones\":[]}"))
                .andExpect(status().isBadRequest());
    }

    private boolean cartaContiene(String token, String producto) throws Exception {
        MvcResult carta = mvc.perform(get("/api/carta").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode categoria : apoyo.leer(carta).get("categorias")) {
            for (JsonNode p : categoria.get("productos")) {
                if (producto.equals(p.get("nombre").asText())) {
                    return true;
                }
            }
        }
        return false;
    }
}
