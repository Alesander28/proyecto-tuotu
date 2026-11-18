package pe.tuotu.sistema.pedidos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Grupo de la carta: pizzas, pastas, parrillas, etc. Cada categoría define cuánto se cobra por llevar. */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    /** Cargo por unidad cuando el producto es para llevar (cero si la categoría no cobra empaque). */
    @Column(name = "recargo_para_llevar", nullable = false, precision = 8, scale = 2,
            columnDefinition = "numeric(8,2) default 0")
    private BigDecimal recargoParaLlevar = BigDecimal.ZERO;

    protected Categoria() {
    }

    public Categoria(String nombre) {
        this.nombre = nombre;
    }

    public Categoria(String nombre, BigDecimal recargoParaLlevar) {
        this.nombre = nombre;
        this.recargoParaLlevar = recargoParaLlevar == null ? BigDecimal.ZERO : recargoParaLlevar;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getRecargoParaLlevar() {
        return recargoParaLlevar == null ? BigDecimal.ZERO : recargoParaLlevar;
    }
}
