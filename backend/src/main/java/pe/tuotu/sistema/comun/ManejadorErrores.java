package pe.tuotu.sistema.comun;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

/** Convierte los errores en respuestas JSON con un campo "mensaje" legible. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> manejarApi(ApiException e) {
        return ResponseEntity.status(e.getEstado()).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .findFirst()
                .orElse("Los datos enviados no son válidos.");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }
}
