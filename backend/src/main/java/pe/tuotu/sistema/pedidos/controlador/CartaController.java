package pe.tuotu.sistema.pedidos.controlador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.pedidos.dto.CartaDto;
import pe.tuotu.sistema.pedidos.servicio.CartaService;

@RestController
@RequestMapping("/api/carta")
public class CartaController {

    private final CartaService cartaService;

    public CartaController(CartaService cartaService) {
        this.cartaService = cartaService;
    }

    @GetMapping
    public CartaDto obtener() {
        return cartaService.obtener();
    }
}
