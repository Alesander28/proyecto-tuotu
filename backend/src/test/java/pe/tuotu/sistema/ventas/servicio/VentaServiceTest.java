package pe.tuotu.sistema.ventas.servicio;

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
import pe.tuotu.sistema.pedidos.dominio.DetallePedido;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import pe.tuotu.sistema.pedidos.repositorio.PedidoRepository;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import pe.tuotu.sistema.ventas.dominio.Comprobante;
import pe.tuotu.sistema.ventas.dominio.Venta;
import pe.tuotu.sistema.ventas.dto.VentaDto;
import pe.tuotu.sistema.ventas.dto.VentaRegistrarRequest;
import pe.tuotu.sistema.ventas.evento.VentaRegistradaEvento;
import pe.tuotu.sistema.ventas.repositorio.ComprobanteRepository;
import pe.tuotu.sistema.ventas.repositorio.VentaRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventas;
    @Mock
    private ComprobanteRepository comprobantes;
    @Mock
    private PedidoRepository pedidos;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private ApplicationEventPublisher publicador;

    private VentaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new VentaService(ventas, comprobantes, pedidos, usuarios, List.of("Efectivo", "Yape"), "B001", publicador);
    }

    private Usuario cajero() {
        Usuario usuario = new Usuario("cajero01", "hash", "Cajero Uno", new Rol("CAJERO", new HashSet<>()));
        ReflectionTestUtils.setField(usuario, "id", 2L);
        return usuario;
    }

    private Pedido pedido(EstadoPedido estado) {
        Usuario mozo = new Usuario("mozo01", "hash", "Mozo Uno", new Rol("MOZO", new HashSet<>()));
        Pedido pedido = new Pedido("5", mozo);
        Producto producto = new Producto("Hawaiana", new Categoria("Pizzas clásicas"));
        Presentacion presentacion = new Presentacion("Personal", new BigDecimal("22.00"), producto);
        pedido.agregarDetalle(new DetallePedido(presentacion, 2, new BigDecimal("25.00"), false, true)); // 50.00
        ReflectionTestUtils.setField(pedido, "id", 9L);
        pedido.setCodigo("0009");
        pedido.setEstado(estado);
        return pedido;
    }

    @Test
    void cobraUnPedidoAtendidoYEmiteLaBoletaConNumeroCorrelativo() {
        when(usuarios.findByNombreUsuario("cajero01")).thenReturn(Optional.of(cajero()));
        when(pedidos.findById(9L)).thenReturn(Optional.of(pedido(EstadoPedido.ATENDIDO)));
        when(ventas.existsByPedidoId(9L)).thenReturn(false);
        when(ventas.save(any(Venta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(comprobantes.save(any(Comprobante.class))).thenAnswer(invocacion -> {
            Comprobante comprobante = invocacion.getArgument(0);
            ReflectionTestUtils.setField(comprobante, "id", 12L);
            return comprobante;
        });

        VentaDto dto = servicio.registrar(new VentaRegistrarRequest(9L, "efectivo"), "cajero01");

        assertEquals(0, new BigDecimal("50.00").compareTo(dto.total()));
        assertEquals("Efectivo", dto.metodoPago());
        assertEquals("B001-00000012", dto.comprobanteNumero());
        assertEquals("0009", dto.pedidoCodigo());
        verify(publicador).publishEvent(any(VentaRegistradaEvento.class));
    }

    @Test
    void noCobraPedidosQueNoEstanAtendidos() {
        when(usuarios.findByNombreUsuario("cajero01")).thenReturn(Optional.of(cajero()));
        when(pedidos.findById(9L)).thenReturn(Optional.of(pedido(EstadoPedido.EN_PREPARACION)));

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.registrar(new VentaRegistrarRequest(9L, "Efectivo"), "cajero01"));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(ventas, never()).save(any());
        verify(publicador, never()).publishEvent(any(VentaRegistradaEvento.class));
    }

    @Test
    void noCobraDosVecesElMismoPedido() {
        when(usuarios.findByNombreUsuario("cajero01")).thenReturn(Optional.of(cajero()));
        when(pedidos.findById(9L)).thenReturn(Optional.of(pedido(EstadoPedido.ATENDIDO)));
        when(ventas.existsByPedidoId(9L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.registrar(new VentaRegistrarRequest(9L, "Efectivo"), "cajero01"));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(ventas, never()).save(any());
    }

    @Test
    void rechazaMetodosDePagoNoPermitidos() {
        when(usuarios.findByNombreUsuario("cajero01")).thenReturn(Optional.of(cajero()));
        when(pedidos.findById(9L)).thenReturn(Optional.of(pedido(EstadoPedido.ATENDIDO)));
        when(ventas.existsByPedidoId(9L)).thenReturn(false);

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.registrar(new VentaRegistrarRequest(9L, "Cheque"), "cajero01"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verify(ventas, never()).save(any());
    }

    @Test
    void rechazaPedidosInexistentes() {
        when(usuarios.findByNombreUsuario("cajero01")).thenReturn(Optional.of(cajero()));
        when(pedidos.findById(99L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.registrar(new VentaRegistrarRequest(99L, "Efectivo"), "cajero01"));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
    }
}
