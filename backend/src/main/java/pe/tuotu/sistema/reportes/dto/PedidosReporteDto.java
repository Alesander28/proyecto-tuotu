package pe.tuotu.sistema.reportes.dto;

import java.time.LocalDate;
import java.util.List;

public record PedidosReporteDto(LocalDate desde, LocalDate hasta, int total, List<CantidadDto> porEstado, List<CantidadDto> porMozo) {
}
