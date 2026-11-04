package pe.tuotu.sistema.inventario.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.inventario.dominio.Insumo;
import pe.tuotu.sistema.inventario.dominio.MovimientoInventario;
import pe.tuotu.sistema.inventario.dominio.TipoMovimiento;
import pe.tuotu.sistema.inventario.dto.InsumoCrearRequest;
import pe.tuotu.sistema.inventario.dto.InsumoDto;
import pe.tuotu.sistema.inventario.dto.MovimientoRequest;
import pe.tuotu.sistema.inventario.repositorio.InsumoRepository;
import pe.tuotu.sistema.inventario.repositorio.MovimientoRepository;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsumoServiceTest {

    @Mock
    private InsumoRepository insumos;
    @Mock
    private MovimientoRepository movimientos;
    @Mock
    private UsuarioRepository usuarios;

    private InsumoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new InsumoService(insumos, movimientos, usuarios);
    }

    private Usuario gerente() {
        Usuario usuario = new Usuario("gerente", "hash", "Gerente", new Rol("GERENTE", new HashSet<>()));
        ReflectionTestUtils.setField(usuario, "id", 1L);
        return usuario;
    }

    private Insumo insumoConStock(String stock) {
        Insumo insumo = new Insumo("Pollo", "kg", new BigDecimal(stock));
        ReflectionTestUtils.setField(insumo, "id", 3L);
        return insumo;
    }

    @Test
    void laEntradaAumentaElStockYRegistraElMovimientoConSuResponsable() {
        Insumo insumo = insumoConStock("10.00");
        when(insumos.buscarParaActualizar(3L)).thenReturn(Optional.of(insumo));
        when(usuarios.findByNombreUsuario("gerente")).thenReturn(Optional.of(gerente()));

        InsumoDto dto = servicio.registrarEntrada(3L, new MovimientoRequest(new BigDecimal("5"), null), "gerente");

        assertEquals(0, new BigDecimal("15.00").compareTo(dto.stockActual()));
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientos).save(captor.capture());
        assertEquals(TipoMovimiento.ENTRADA, captor.getValue().getTipo());
        assertEquals("Ingreso de mercadería", captor.getValue().getMotivo());
        assertEquals("gerente", captor.getValue().getUsuario().getNombreUsuario());
    }

    @Test
    void laSalidaDisminuyeElStock() {
        Insumo insumo = insumoConStock("10.00");
        when(insumos.buscarParaActualizar(3L)).thenReturn(Optional.of(insumo));
        when(usuarios.findByNombreUsuario("gerente")).thenReturn(Optional.of(gerente()));

        InsumoDto dto = servicio.registrarSalida(3L, new MovimientoRequest(new BigDecimal("4"), "Consumo"), "gerente");

        assertEquals(0, new BigDecimal("6.00").compareTo(dto.stockActual()));
    }

    @Test
    void laSalidaNoPuedeDejarElStockEnNegativo() {
        Insumo insumo = insumoConStock("3.00");
        when(insumos.buscarParaActualizar(3L)).thenReturn(Optional.of(insumo));
        when(usuarios.findByNombreUsuario("gerente")).thenReturn(Optional.of(gerente()));

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.registrarSalida(3L, new MovimientoRequest(new BigDecimal("8"), "Consumo"), "gerente"));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        assertEquals(0, new BigDecimal("3.00").compareTo(insumo.getStockActual()));
        verify(movimientos, never()).save(any());
    }

    @Test
    void laSalidaExigeUnMotivo() {
        Insumo insumo = insumoConStock("10.00");
        when(insumos.buscarParaActualizar(3L)).thenReturn(Optional.of(insumo));
        when(usuarios.findByNombreUsuario("gerente")).thenReturn(Optional.of(gerente()));

        ApiException error = assertThrows(ApiException.class,
                () -> servicio.registrarSalida(3L, new MovimientoRequest(new BigDecimal("1"), "  "), "gerente"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verify(movimientos, never()).save(any());
    }

    @Test
    void noPermiteInsumosRepetidos() {
        when(insumos.existsByNombreIgnoreCase("Pollo")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> servicio.crear(new InsumoCrearRequest("Pollo", "kg")));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(insumos, never()).save(any());
    }
}
