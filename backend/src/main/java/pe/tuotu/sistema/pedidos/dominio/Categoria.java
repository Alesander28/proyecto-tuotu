package pe.tuotu.sistema.pedidos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Grupo de la carta: pizzas clásicas, pizzas especiales, alitas, etc. */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    protected Categoria() {
    }

    public Categoria(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
