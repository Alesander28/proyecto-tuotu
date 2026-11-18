package pe.tuotu.sistema.reportes;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de los reportes (HU-015). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReporteApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private ApiTestSupport apoyo;

    @BeforeEach
    void preparar() {
        apoyo = new ApiTestSupport(mvc, json);
    }

    @Test
    void elReporteDeVentasIncluyeLasVentasDelDia() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_rep1", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_rep1", "CAJERO");
        mvc.perform(post("/api/cajas/abrir").header("Authorization", "Bearer " + tokenCajero)
                .contentType(MediaType.APPLICATION_JSON).content("{\"montoInicial\":100}")).andExpect(status().isCreated());
        long pedidoId = apoyo.crearPedidoAtendido(tokenMozo);
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":" + pedidoId + ",\"metodoPago\":\"Efectivo\"}"))
                .andExpect(status().isCreated());

        MvcResult resultado = mvc.perform(get("/api/reportes/ventas").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isOk()).andReturn();
        JsonNode reporte = apoyo.leer(resultado);

        assertTrue(reporte.get("cantidadVentas").asInt() >= 1);
        assertTrue(reporte.get("totalVendido").asDouble() > 0);
        assertFalse(reporte.get("porCategoria").isEmpty());
        assertFalse(reporte.get("masVendidos").isEmpty());
    }

    @Test
    void elReporteDeInventarioListaLosInsumos() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/reportes/inventario").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isOk()).andReturn();

        assertFalse(apoyo.leer(resultado).get("insumos").isEmpty());
    }

    @Test
    void elResumenDelDiaTraeLasCifrasDelPanel() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_rep3", "MOZO");
        apoyo.crearPedido(tokenMozo);

        MvcResult resultado = mvc.perform(get("/api/reportes/resumen").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isOk()).andReturn();
        JsonNode resumen = apoyo.leer(resultado);

        assertTrue(resumen.get("pedidosHoy").asInt() >= 1);
        assertTrue(resumen.get("pedidosPendientes").asInt() >= 1);
        assertTrue(resumen.has("alertasStock"));
        assertTrue(resumen.has("cajasAbiertas"));
    }

    @Test
    void laSerieDeVentasIncluyeTodosLosDiasDelRango() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/reportes/serie").param("desde", "2026-01-01").param("hasta", "2026-01-10")
                        .param("agrupar", "dia").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isOk()).andReturn();

        assertEquals(10, apoyo.leer(resultado).size());
    }

    @Test
    void laSerieRechazaUnaAgrupacionInvalida() throws Exception {
        mvc.perform(get("/api/reportes/serie").param("agrupar", "siglo").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elReporteDePedidosSeparaPorEstadoYPorMozo() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_rep4", "MOZO");
        apoyo.crearPedido(tokenMozo);

        MvcResult resultado = mvc.perform(get("/api/reportes/pedidos").header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isOk()).andReturn();
        JsonNode reporte = apoyo.leer(resultado);

        assertEquals(3, reporte.get("porEstado").size());
        assertFalse(reporte.get("porMozo").isEmpty());
    }

    @Test
    void soloElGerenteVeLosReportes() throws Exception {
        String tokenCajero = apoyo.sesionDe("cajero_rep2", "CAJERO");

        mvc.perform(get("/api/reportes/ventas").header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isForbidden());
    }

    @Test
    void laFechaFinalNoPuedeSerAnteriorALaInicial() throws Exception {
        mvc.perform(get("/api/reportes/ventas").param("desde", "2026-10-20").param("hasta", "2026-10-10")
                        .header("Authorization", "Bearer " + apoyo.tokenGerente()))
                .andExpect(status().isBadRequest());
    }
}
