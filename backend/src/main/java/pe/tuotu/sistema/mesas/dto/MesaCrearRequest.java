package pe.tuotu.sistema.mesas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MesaCrearRequest(
        @NotBlank(message = "Indique el número o nombre de la mesa.")
        @Size(max = 10, message = "El número de la mesa es demasiado largo.")
        String numero) {
}
