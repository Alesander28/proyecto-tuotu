package pe.tuotu.sistema.pedidos.dto;

import java.math.BigDecimal;

/** El cargo de extra queso es nulo cuando la presentación no lo admite. */
public record PresentacionDto(Long id, String nombre, BigDecimal precio, BigDecimal recargoExtraQueso) {
}
