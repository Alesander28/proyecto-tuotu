package pe.tuotu.sistema.pedidos.dto;

import java.util.List;

/** La carta con sus categorías. Los cargos adicionales vienen en cada categoría y en cada presentación. */
public record CartaDto(List<CategoriaCartaDto> categorias) {
}
