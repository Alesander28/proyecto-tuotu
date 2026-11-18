package pe.tuotu.sistema.auditoria.dto;

import java.time.LocalDateTime;

public record RegistroAuditoriaDto(Long id, LocalDateTime fechaHora, String usuario, String modulo, String tipo,
                                   String descripcion, String referencia) {
}
