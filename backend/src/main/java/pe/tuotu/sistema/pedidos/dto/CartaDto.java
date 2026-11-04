package pe.tuotu.sistema.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

/** La carta con sus categorías y los cargos adicionales, para que la pantalla calcule el mismo total que el servidor. */
public record CartaDto(List<CategoriaCartaDto> categorias, BigDecimal recargoExtraQueso, BigDecimal recargoParaLlevar) {
}
