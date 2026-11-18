package pe.tuotu.sistema.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ventas del periodo. «totalVendido» es lo cobrado (ingresos). El sistema no tiene costos de productos ni de
 * insumos, por lo que no calcula ganancias.
 */
public record VentasReporteDto(
        LocalDate desde,
        LocalDate hasta,
        int cantidadVentas,
        BigDecimal totalVendido,
        List<MontoDto> porCategoria,
        List<CantidadDto> masVendidos,
        List<MontoDto> porMetodoPago,
        List<MontoDto> porCajero,
        BigDecimal ticketPromedio) {
}
