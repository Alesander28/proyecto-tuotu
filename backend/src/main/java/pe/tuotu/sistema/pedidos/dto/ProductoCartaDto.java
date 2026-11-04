package pe.tuotu.sistema.pedidos.dto;

import java.util.List;

public record ProductoCartaDto(Long id, String nombre, List<PresentacionDto> presentaciones) {
}
