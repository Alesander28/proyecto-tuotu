package pe.tuotu.sistema.reportes.dto;

import java.math.BigDecimal;
import java.util.List;

/** Cifras del día para el panel del gerente. Todos los valores se calculan con datos reales del sistema. */
public record ResumenDto(
        long pedidosHoy,
        long pedidosAyer,
        int ventasHoyCantidad,
        BigDecimal ventasHoyMonto,
        BigDecimal ventasAyerMonto,
        BigDecimal ticketPromedio,
        int mesasAtendidasHoy,
        long pedidosPendientes,
        long pedidosEnPreparacion,
        int insumosStockBajo,
        int insumosAgotados,
        long cajasAbiertas,
        List<AlertaStockDto> alertasStock) {
}
