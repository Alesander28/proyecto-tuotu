package pe.tuotu.sistema.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un punto de la serie de ventas: el periodo comienza en «periodo» (día, lunes de la semana o primer día del mes). */
public record PuntoSerieDto(LocalDate periodo, int ventas, BigDecimal monto) {
}
