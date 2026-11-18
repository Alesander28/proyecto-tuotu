package pe.tuotu.sistema.comun;

import org.springframework.http.HttpStatus;

/** Error de negocio con el código HTTP que debe devolverse. */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;

    public ApiException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
