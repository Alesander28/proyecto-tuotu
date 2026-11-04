package pe.tuotu.sistema.comun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Utilidades compartidas por las pruebas de la API: iniciar sesión y crear usuarios con un rol. */
public class ApiTestSupport {

    public static final String CLAVE_GERENTE = "Gerente123*";

    private final MockMvc mvc;
    private final ObjectMapper json;

    public ApiTestSupport(MockMvc mvc, ObjectMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    public String iniciarSesion(String usuario, String clave) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"" + clave + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    public String tokenGerente() throws Exception {
        return iniciarSesion("gerente", CLAVE_GERENTE);
    }

    private long idDelRol(String tokenGerente, String nombre) throws Exception {
        MvcResult r = mvc.perform(get("/api/roles").header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode rol : json.readTree(r.getResponse().getContentAsString())) {
            if (nombre.equals(rol.get("nombre").asText())) {
                return rol.get("id").asLong();
            }
        }
        throw new IllegalStateException("No existe el rol " + nombre);
    }

    /** Crea un usuario con el rol indicado (lo hace el gerente) y devuelve su token de sesión. */
    public String sesionDe(String usuario, String rol) throws Exception {
        String tokenGerente = tokenGerente();
        long rolId = idDelRol(tokenGerente, rol);
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"clave123\","
                                + "\"nombreCompleto\":\"Usuario de prueba\",\"rolId\":" + rolId + "}"))
                .andExpect(status().isCreated());
        return iniciarSesion(usuario, "clave123");
    }

    /** Registra un pedido de una unidad con el primer producto de la carta; devuelve su id. */
    public long crearPedido(String tokenMozo) throws Exception {
        MvcResult carta = mvc.perform(get("/api/carta").header("Authorization", "Bearer " + tokenMozo))
                .andExpect(status().isOk()).andReturn();
        long presentacionId = leer(carta).get("categorias").get(0).get("productos").get(0).get("presentaciones").get(0).get("id").asLong();
        MvcResult pedido = mvc.perform(post("/api/pedidos").header("Authorization", "Bearer " + tokenMozo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesa\":\"7\",\"items\":[{\"presentacionId\":" + presentacionId
                                + ",\"cantidad\":2,\"paraLlevar\":false,\"extraQueso\":false}]}"))
                .andExpect(status().isCreated()).andReturn();
        return leer(pedido).get("id").asLong();
    }

    public void cambiarEstado(String token, long idPedido, String estado) throws Exception {
        mvc.perform(patch("/api/pedidos/" + idPedido + "/estado").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"" + estado + "\"}"))
                .andExpect(status().isOk());
    }

    /** Registra un pedido y lo deja en estado ATENDIDO, listo para cobrar. */
    public long crearPedidoAtendido(String tokenMozo) throws Exception {
        long id = crearPedido(tokenMozo);
        cambiarEstado(tokenMozo, id, "EN_PREPARACION");
        cambiarEstado(tokenMozo, id, "ATENDIDO");
        return id;
    }

    public JsonNode leer(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }
}
