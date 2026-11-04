package pe.tuotu.sistema.reportes.dto;

import java.time.LocalDate;
import java.util.List;

public record InventarioReporteDto(LocalDate desde, LocalDate hasta, List<InsumoReporteDto> insumos) {
}
