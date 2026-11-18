package pe.tuotu.sistema.cajas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Los montos de ventas se calculan con las ventas que el cajero registró durante el turno. En una caja abierta,
 * «efectivoEsperado» es lo que debería haber en caja hasta ahora (monto inicial más ventas en efectivo).
 */
public record CajaDto(Long id, String cajero, String cajeroUsuario, LocalDateTime fechaApertura, BigDecimal montoInicial,
                      LocalDateTime fechaCierre, String estado, int cantidadVentas, BigDecimal totalVentas,
                      BigDecimal ventasEfectivo, BigDecimal efectivoEsperado, BigDecimal montoContado,
                      BigDecimal diferencia, String observacion) {
}
