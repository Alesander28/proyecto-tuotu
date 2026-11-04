package pe.tuotu.sistema.seguridad;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la API completa con una base de datos en memoria: comprueban RNF-01, RNF-02 y las HU del Sprint 1. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SeguridadApiTest {

    private static final String CLAVE_GERENTE = "Gerente123*";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private String iniciarSesion(String usuario, String clave) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private long idDelRol(String token, String nombre) throws Exception {
        MvcResult resultado = mvc.perform(get("/api/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode rol : json.readTree(resultado.getResponse().getContentAsString())) {
            if (nombre.equals(rol.get("nombre").asText())) {
                return rol.get("id").asLong();
            }
        }
        throw new IllegalStateException("No existe el rol " + nombre);
    }

    private long crearUsuario(String tokenGerente, String usuario, String clave, String rol) throws Exception {
        long rolId = idDelRol(tokenGerente, rol);
        MvcResult resultado = mvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario + "\",\"contrasena\":\"" + clave
                                + "\",\"nombreCompleto\":\"Usuario de prueba\",\"rolId\":" + rolId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void sinTokenSeRechazaElAcceso() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void credencialesIncorrectasDevuelvenUnMensajeQueNoRevelaQueDatoFallo() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"gerente\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void elGerenteListaLosUsuarios() throws Exception {
        String token = iniciarSesion("gerente", CLAVE_GERENTE);
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unMozoNoPuedeGestionarUsuarios() throws Exception {
        String tokenGerente = iniciarSesion("gerente", CLAVE_GERENTE);
        crearUsuario(tokenGerente, "mozo_prueba1", "clave123", "MOZO");
        String tokenMozo = iniciarSesion("mozo_prueba1", "clave123");

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenMozo))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSePermiteRepetirElNombreDeUsuario() throws Exception {
        String tokenGerente = iniciarSesion("gerente", CLAVE_GERENTE);
        crearUsuario(tokenGerente, "cajero_prueba1", "clave123", "CAJERO");
        long rolId = idDelRol(tokenGerente, "CAJERO");

        mvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"cajero_prueba1\",\"contrasena\":\"clave123\","
                                + "\"nombreCompleto\":\"Otro\",\"rolId\":" + rolId + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cerrarSesionInvalidaElToken() throws Exception {
        String token = iniciarSesion("gerente", CLAVE_GERENTE);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void desactivarUnUsuarioBloqueaSuAccesoDeInmediato() throws Exception {
        String tokenGerente = iniciarSesion("gerente", CLAVE_GERENTE);
        long id = crearUsuario(tokenGerente, "mozo_prueba2", "clave123", "MOZO");
        String tokenMozo = iniciarSesion("mozo_prueba2", "clave123");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenMozo)).andExpect(status().isOk());

        mvc.perform(patch("/api/usuarios/" + id + "/desactivar").header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenMozo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"mozo_prueba2\",\"contrasena\":\"clave123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
