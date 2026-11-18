package pe.tuotu.sistema.pedidos.dominio;

import java.util.Optional;

/** Estados de un pedido, en el orden en que avanza: pendiente, en preparación y atendido. */
public enum EstadoPedido {
    PENDIENTE("pendiente"),
    EN_PREPARACION("en preparación"),
    ATENDIDO("atendido");

    private final String texto;

    EstadoPedido(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }

    /** El único estado al que puede pasar un pedido desde este (vacío si ya está atendido). */
    public Optional<EstadoPedido> siguiente() {
        EstadoPedido[] todos = values();
        return ordinal() + 1 < todos.length ? Optional.of(todos[ordinal() + 1]) : Optional.empty();
    }
}
