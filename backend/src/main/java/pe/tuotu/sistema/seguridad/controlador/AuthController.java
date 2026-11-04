package pe.tuotu.sistema.seguridad.controlador;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.seguridad.dto.LoginRequest;
import pe.tuotu.sistema.seguridad.dto.LoginResponse;
import pe.tuotu.sistema.seguridad.dto.UsuarioDto;
import pe.tuotu.sistema.seguridad.servicio.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest solicitud) {
        return authService.login(solicitud);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String cabecera) {
        String token = cabecera.startsWith("Bearer ") ? cabecera.substring(7).trim() : cabecera.trim();
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioDto actual(Authentication autenticacion) {
        return authService.actual(autenticacion.getName());
    }
}
