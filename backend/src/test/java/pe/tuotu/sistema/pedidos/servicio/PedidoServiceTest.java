package pe.tuotu.sistema.pedidos.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.pedidos.dominio.Categoria;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import pe.tuotu.sistema.pedidos.dto.ItemPedidoRequest;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;
import pe.tuotu.sistema.pedidos.dto.PedidoRegistrarRequest;
import pe.tuotu.sistema.pedidos.evento.PedidoEstadoCambiadoEvento;
import pe.tuotu.sistema.pedidos.repositorio.PedidoRepository;
import pe.tuotu.sistema.pedidos.repositorio.PresentacionRepository;
import pe.tuotu.sistema.pedidos.servicio.recargo.RecargoExtraQueso;
import pe.tuotu.sistema.pedidos.servicio.recargo.RecargoParaLlevar;
import pe.tuotu.sistema.pedidos.servicio.recargo.RecargoPedido;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidos;
    @Mock
    private PresentacionRepository presentaciones;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private ApplicationEventPublisher publicador;

    private PedidoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new PedidoService(pedidos, presentaciones, usuarios,
                List.of(new RecargoExtraQueso(new BigDecimal("3.00")), new RecargoParaLlevar(new BigDecimal("2.00"))), publicador);
    }

    private Usuario mozo() {
        Usuario usuario = new Usuario("mozo01", "hash", "Mozo Uno", new Rol("MOZO", new HashSet<>()));
        ReflectionTestUtils.setField(usuario, "id", 5L);
        return usuario;
    }

    private Presentacion presentacion(long id, String nombre, String precio, boolean productoActivo) {
        Producto producto = new Producto("Hawaiana", new Categoria("Pizzas clásicas"));
        producto.setActivo(productoActivo);
        Presentacion presentacion = new Presentacion(nombre, new BigDecimal(precio), producto);
        ReflectionTestUtils.setField(presentacion, "id", id);
        return presentacion;
    }

    private Pedido pedidoEn(EstadoPedido estado) {
        Pedido pedido = new Pedido("5", mozo());
        ReflectionTestUtils.setField(pedido, "id", 7L);
        pedido.setEstado(estado);
        return pedido;
    }

    @Test
    void calculaElTotalConLosCargosAdicionalesYQuedaPendiente() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(mozo()));
        when(presentaciones.findById(1L)).thenReturn(Optional.of(presentacion(1L, "Personal", "22.00", true)));
        when(presentaciones.findById(2L)).thenReturn(Optional.of(presentacion(2L, "Familiar", "42.00", true)));
        when(pedidos.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 7L);
            return pedido;
        });

        PedidoRegistrarRequest solicitud = new PedidoRegistrarRequest("5", List.of(
                new ItemPedidoRequest(1L, 2, true, true),    // (22 + 3 + 2) x 2 = 54
                new ItemPedidoRequest(2L, 1, false, false))); // 42

        PedidoDto dto = servicio.registrar(solicitud, "mozo01");

        assertEquals(0, new BigDecimal("96.00").compareTo(dto.total()));
        assertEquals("PENDIENTE", dto.estado());
        assertEquals("0007", dto.codigo());
        assertEquals(2, dto.detalles().size());
    }

    @Test
    void rechazaProductosNoDisponibles() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(mozo()));
        when(presentaciones.findById(1L)).thenReturn(Optional.of(presentacion(1L, "Personal", "22.00", false)));

        PedidoRegistrarRequest solicitud = new PedidoRegistrarRequest("5", List.of(new ItemPedidoRequest(1L, 1, false, false)));

        ApiException error = assertThrows(ApiException.class, () -> servicio.registrar(solicitud, "mozo01"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verify(pedidos, never()).save(any());
    }

    @Test
    void rechazaProductosQueNoExistenEnLaCarta() {
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(mozo()));
        when(presentaciones.findById(99L)).thenReturn(Optional.empty());

        PedidoRegistrarRequest solicitud = new PedidoRegistrarRequest("5", List.of(new ItemPedidoRequest(99L, 1, false, false)));

        ApiException error = assertThrows(ApiException.class, () -> servicio.registrar(solicitud, "mozo01"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
    }

    @Test
    void elPedidoAvanzaDePendienteAEnPreparacionYLuegoAAtendido() {
        Pedido pedido = pedidoEn(EstadoPedido.PENDIENTE);
        when(pedidos.findById(7L)).thenReturn(Optional.of(pedido));

        assertEquals("EN_PREPARACION", servicio.cambiarEstado(7L, EstadoPedido.EN_PREPARACION).estado());
        assertEquals("ATENDIDO", servicio.cambiarEstado(7L, EstadoPedido.ATENDIDO).estado());
        verify(publicador, times(2)).publishEvent(any(PedidoEstadoCambiadoEvento.class));
    }

    @Test
    void unCambioDeEstadoNoPermitidoNoPublicaNingunEvento() {
        when(pedidos.findById(7L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.PENDIENTE)));

        assertThrows(ApiException.class, () -> servicio.cambiarEstado(7L, EstadoPedido.ATENDIDO));

        verify(publicador, never()).publishEvent(any(PedidoEstadoCambiadoEvento.class));
    }

    /** Requisito nuevo: un cargo adicional. Se agrega como una estrategia más; PedidoService no se modifica. */
    @Test
    void unRecargoNuevoSeIncorporaSinModificarElServicio() {
        RecargoPedido empaqueEspecial = item -> new BigDecimal("1.00");
        PedidoService conRecargoNuevo = new PedidoService(pedidos, presentaciones, usuarios,
                List.of(new RecargoExtraQueso(new BigDecimal("3.00")), new RecargoParaLlevar(new BigDecimal("2.00")), empaqueEspecial),
                publicador);
        when(usuarios.findByNombreUsuario("mozo01")).thenReturn(Optional.of(mozo()));
        when(presentaciones.findById(1L)).thenReturn(Optional.of(presentacion(1L, "Personal", "22.00", true)));
        when(pedidos.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 8L);
            return pedido;
        });

        PedidoDto dto = conRecargoNuevo.registrar(
                new PedidoRegistrarRequest("3", List.of(new ItemPedidoRequest(1L, 1, false, false))), "mozo01");

        assertEquals(0, new BigDecimal("23.00").compareTo(dto.total())); // 22.00 + 1.00 del cargo nuevo
    }

    @Test
    void noSePermiteSaltarDeEstado() {
        when(pedidos.findById(7L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.PENDIENTE)));

        ApiException error = assertThrows(ApiException.class, () -> servicio.cambiarEstado(7L, EstadoPedido.ATENDIDO));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
    }

    @Test
    void noSePermiteRetrocederDeEstado() {
        when(pedidos.findById(7L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.EN_PREPARACION)));

        ApiException error = assertThrows(ApiException.class, () -> servicio.cambiarEstado(7L, EstadoPedido.PENDIENTE));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
    }

    @Test
    void unPedidoAtendidoYaNoCambia() {
        when(pedidos.findById(7L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.ATENDIDO)));

        ApiException error = assertThrows(ApiException.class, () -> servicio.cambiarEstado(7L, EstadoPedido.ATENDIDO));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
    }
}
