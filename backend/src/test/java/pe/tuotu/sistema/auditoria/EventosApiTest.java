package pe.tuotu.sistema.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import pe.tuotu.sistema.comun.ApiTestSupport;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.evento.PedidoEstadoCambiadoEvento;
import pe.tuotu.sistema.ventas.evento.VentaRegistradaEvento;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comprueba que los servicios publican los eventos que reciben los observadores (patrón Observer). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@RecordApplicationEvents
class EventosApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private ApplicationEvents eventos;

    private ApiTestSupport apoyo;

    @BeforeEach
    void preparar() {
        apoyo = new ApiTestSupport(mvc, json);
    }

    @Test
    void cadaCambioDeEstadoDelPedidoPublicaUnEvento() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_aud1", "MOZO");
        long id = apoyo.crearPedido(tokenMozo);

        apoyo.cambiarEstado(tokenMozo, id, "EN_PREPARACION");
        apoyo.cambiarEstado(tokenMozo, id, "ATENDIDO");

        List<PedidoEstadoCambiadoEvento> cambios = eventos.stream(PedidoEstadoCambiadoEvento.class)
                .filter(e -> e.pedidoId().equals(id)).toList();
        assertEquals(2, cambios.size());
        assertEquals(EstadoPedido.PENDIENTE, cambios.get(0).anterior());
        assertEquals(EstadoPedido.EN_PREPARACION, cambios.get(0).nuevo());
        assertEquals(EstadoPedido.ATENDIDO, cambios.get(1).nuevo());
    }

    @Test
    void cobrarUnPedidoPublicaElEventoDeVenta() throws Exception {
        String tokenMozo = apoyo.sesionDe("mozo_aud2", "MOZO");
        String tokenCajero = apoyo.sesionDe("cajero_aud2", "CAJERO");
        long id = apoyo.crearPedidoAtendido(tokenMozo);

        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":" + id + ",\"metodoPago\":\"Efectivo\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, eventos.stream(VentaRegistradaEvento.class).count());
    }
}
