package pe.tuotu.sistema.ventas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Boleta de consumo de una venta, con numeración correlativa. Es de control interno: no tiene validez tributaria. */
@Entity
@Table(name = "comprobante")
public class Comprobante {

    public static final String TIPO_BOLETA = "BOLETA DE CONSUMO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_venta", unique = true)
    private Venta venta;

    @Column(unique = true, length = 20)
    private String numero;

    @Column(nullable = false, length = 20)
    private String tipo = TIPO_BOLETA;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();

    protected Comprobante() {
    }

    public Comprobante(Venta venta) {
        this.venta = venta;
    }

    public Long getId() {
        return id;
    }

    public Venta getVenta() {
        return venta;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }
}
