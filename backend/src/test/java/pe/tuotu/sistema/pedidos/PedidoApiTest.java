package pe.tuotu.sistema.pedidos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la API de pedidos con una base de datos en memoria (HU-007 y HU-008). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PedidoApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private String iniciarSesion(String usuario, String clave) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"" + clave + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private long idDelRol(String token, String nombre) throws Exception {
        MvcResult r = mvc.perform(get("/api/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode rol : json.readTree(r.getResponse().getContentAsString())) {
            if (nombre.equals(rol.get("nombre").asText())) {
                return rol.get("id").asLong();
            }
        }
        throw new IllegalStateException("No existe el rol " + nombre);
    }

    /** Crea un usuario con el rol indicado (lo hace el gerente) y devuelve su token de sesión. */
    private String sesionDe(String usuario, String rol) throws Exception {
        String tokenGerente = iniciarSesion("gerente", "Gerente123*");
        long rolId = idDelRol(tokenGerente, rol);
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"clave123\","
                                + "\"nombreCompleto\":\"Usuario de prueba\",\"rolId\":" + rolId + "}"))
                .andExpect(status().isCreated());
        return iniciarSesion(usuario, "clave123");
    }

    /** Devuelve la primera presentación de la carta como [id, precio]. */
    private String[] primeraPresentacion(String token) throws Exception {
        MvcResult r = mvc.perform(get("/api/carta").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode presentacion = json.readTree(r.getResponse().getContentAsString())
                .get("categorias").get(0).get("productos").get(0).get("presentaciones").get(0);
        return new String[]{presentacion.get("id").asText(), presentacion.get("precio").asText()};
    }

    private MvcResult registrar(String token, String cuerpo) throws Exception {
        return mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    @Test
    void elMozoRegistraUnPedidoPendienteConSuCodigoYTotal() throws Exception {
        String token = sesionDe("mozo_ped1", "MOZO");
        String[] pres = primeraPresentacion(token);

        MvcResult r = registrar(token, "{\"mesa\":\"5\",\"items\":[{\"presentacionId\":" + pres[0]
                + ",\"cantidad\":2,\"paraLlevar\":false,\"extraQueso\":true}]}");

        assertEquals(201, r.getResponse().getStatus());
        JsonNode pedido = json.readTree(r.getResponse().getContentAsString());
        assertEquals("PENDIENTE", pedido.get("estado").asText());
        assertNotNull(pedido.get("codigo").asText());
        BigDecimal esperado = new BigDecimal(pres[1]).add(new BigDecimal("3.00")).multiply(BigDecimal.valueOf(2));
        assertEquals(0, esperado.compareTo(new BigDecimal(pedido.get("total").asText())));
    }

    @Test
    void unCajeroNoPuedeRegistrarPedidos() throws Exception {
        String token = sesionDe("cajero_ped1", "CAJERO");
        String[] pres = primeraPresentacion(token);

        MvcResult r = registrar(token, "{\"mesa\":\"1\",\"items\":[{\"presentacionId\":" + pres[0]
                + ",\"cantidad\":1,\"paraLlevar\":false,\"extraQueso\":false}]}");

        assertEquals(403, r.getResponse().getStatus());
    }

    @Test
    void noSeAceptaUnPedidoSinProductos() throws Exception {
        String token = sesionDe("mozo_ped2", "MOZO");

        mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mesa\":\"3\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El pedido debe tener al menos un producto."));
    }

    @Test
    void elCajeroVeLosPedidosPeroNoCambiaSuEstado() throws Exception {
        String tokenMozo = sesionDe("mozo_ped3", "MOZO");
        String tokenCajero = sesionDe("cajero_ped2", "CAJERO");
        String[] pres = primeraPresentacion(tokenMozo);
        MvcResult creado = registrar(tokenMozo, "{\"mesa\":\"2\",\"items\":[{\"presentacionId\":" + pres[0]
                + ",\"cantidad\":1,\"paraLlevar\":false,\"extraQueso\":false}]}");
        long id = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer " + tokenCajero)).andExpect(status().isOk());
        mvc.perform(patch("/api/pedidos/" + id + "/estado").header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elPedidoSoloAvanzaEnOrden() throws Exception {
        String token = sesionDe("mozo_ped4", "MOZO");
        String[] pres = primeraPresentacion(token);
        MvcResult creado = registrar(token, "{\"mesa\":\"4\",\"items\":[{\"presentacionId\":" + pres[0]
                + ",\"cantidad\":1,\"paraLlevar\":true,\"extraQueso\":false}]}");
        long id = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
        String ruta = "/api/pedidos/" + id + "/estado";

        // saltar de pendiente a atendido no se permite
        mvc.perform(patch(ruta).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ATENDIDO\"}")).andExpect(status().isConflict());
        // el orden correcto sí
        mvc.perform(patch(ruta).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
        mvc.perform(patch(ruta).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ATENDIDO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ATENDIDO"));
    }
}
