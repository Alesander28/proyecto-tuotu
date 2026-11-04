package pe.tuotu.sistema.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VentasReporteDto(
        LocalDate desde,
        LocalDate hasta,
        int cantidadVentas,
        BigDecimal totalVendido,
        List<MontoDto> porCategoria,
        List<CantidadDto> masVendidos,
        List<MontoDto> porMetodoPago) {
}
