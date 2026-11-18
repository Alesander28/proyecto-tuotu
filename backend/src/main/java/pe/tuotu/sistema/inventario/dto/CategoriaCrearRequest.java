package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CategoriaCrearRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio.")
        @Size(max = 50, message = "El nombre de la categoría es demasiado largo.")
        String nombre,

        /** Opcional: cargo por unidad cuando el producto es para llevar. Si no se indica, no se cobra. */
        @DecimalMin(value = "0.00", message = "El cargo para llevar no puede ser negativo.")
        BigDecimal recargoParaLlevar) {
}
