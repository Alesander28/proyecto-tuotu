package pe.tuotu.sistema.pedidos.dto;

import java.math.BigDecimal;

public record PresentacionDto(Long id, String nombre, BigDecimal precio) {
}
