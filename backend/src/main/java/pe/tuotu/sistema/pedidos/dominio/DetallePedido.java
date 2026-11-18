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

/**
 * Cada producto pedido. El precio unitario se guarda aquí (ya con los cargos de extra queso y para llevar),
 * para que un cambio posterior de precios no altere pedidos ya hechos.
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_presentacion")
    private Presentacion presentacion;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 8, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "para_llevar", nullable = false)
    private boolean paraLlevar;

    @Column(name = "extra_queso", nullable = false)
    private boolean extraQueso;

    protected DetallePedido() {
    }

    public DetallePedido(Presentacion presentacion, int cantidad, BigDecimal precioUnitario,
                         boolean paraLlevar, boolean extraQueso) {
        this.presentacion = presentacion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.paraLlevar = paraLlevar;
        this.extraQueso = extraQueso;
    }

    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Long getId() {
        return id;
    }

    public Presentacion getPresentacion() {
        return presentacion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public boolean isParaLlevar() {
        return paraLlevar;
    }

    public boolean isExtraQueso() {
        return extraQueso;
    }
}
