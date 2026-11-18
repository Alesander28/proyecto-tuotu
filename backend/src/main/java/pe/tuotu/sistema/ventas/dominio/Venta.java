package pe.tuotu.sistema.ventas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.seguridad.dominio.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cobro de un pedido atendido, registrado por el cajero. Un pedido solo puede tener una venta. */
@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_pedido", unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario cajero;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    protected Venta() {
    }

    public Venta(Pedido pedido, Usuario cajero, String metodoPago, BigDecimal total) {
        this.pedido = pedido;
        this.cajero = cajero;
        this.metodoPago = metodoPago;
        this.total = total;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Usuario getCajero() {
        return cajero;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
