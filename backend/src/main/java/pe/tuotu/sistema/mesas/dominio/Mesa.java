package pe.tuotu.sistema.mesas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mesa del salón. El estado (libre u ocupada) no se guarda: se calcula a partir de los pedidos abiertos. */
@Entity
@Table(name = "mesa")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mesa")
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String numero;

    @Column(nullable = false)
    private boolean activa = true;

    protected Mesa() {
    }

    public Mesa(String numero) {
        this.numero = numero;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
