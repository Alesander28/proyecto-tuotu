package pe.tuotu.sistema.pedidos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Forma en que se vende un producto (personal, familiar, 6 unidades...) con su precio. */
@Entity
@Table(name = "presentacion")
public class Presentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_presentacion")
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal precio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    protected Presentacion() {
    }

    public Presentacion(String nombre, BigDecimal precio, Producto producto) {
        this.nombre = nombre;
        this.precio = precio;
        this.producto = producto;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Producto getProducto() {
        return producto;
    }
}
