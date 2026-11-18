package pe.tuotu.sistema.mesas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Estado LIBRE u OCUPADA. Una mesa está ocupada mientras tenga pedidos sin cobrar. */
public record MesaDto(Long id, String numero, boolean activa, String estado, int pedidosAbiertos, String pedidoCodigo,
                      String pedidoEstado, String mozo, LocalDateTime ocupadaDesde, Long minutosOcupada,
                      BigDecimal totalAbierto) {
}
