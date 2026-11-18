package pe.tuotu.sistema.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Acción concreta que un rol puede realizar (por ejemplo, USUARIOS_GESTIONAR). */
@Entity
@Table(name = "permiso")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(length = 120)
    private String descripcion;

    protected Permiso() {
    }

    public Permiso(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
