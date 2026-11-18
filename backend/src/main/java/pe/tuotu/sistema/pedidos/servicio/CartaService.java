package pe.tuotu.sistema.pedidos.servicio;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.pedidos.dominio.Categoria;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import pe.tuotu.sistema.pedidos.dto.CartaDto;
import pe.tuotu.sistema.pedidos.dto.CategoriaCartaDto;
import pe.tuotu.sistema.pedidos.dto.PresentacionDto;
import pe.tuotu.sistema.pedidos.dto.ProductoCartaDto;
import pe.tuotu.sistema.pedidos.repositorio.CategoriaRepository;
import pe.tuotu.sistema.pedidos.repositorio.PresentacionRepository;
import pe.tuotu.sistema.pedidos.repositorio.ProductoRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Entrega la carta (solo productos activos) para armar pedidos, en el mismo orden de la carta impresa. */
@Service
public class CartaService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final PresentacionRepository presentaciones;

    public CartaService(CategoriaRepository categorias, ProductoRepository productos,
                        PresentacionRepository presentaciones) {
        this.categorias = categorias;
        this.productos = productos;
        this.presentaciones = presentaciones;
    }

    @Transactional(readOnly = true)
    public CartaDto obtener() {
        Map<Long, List<PresentacionDto>> porProducto = new LinkedHashMap<>();
        for (Presentacion p : presentaciones.findAllByOrderByPrecioAsc()) {
            porProducto.computeIfAbsent(p.getProducto().getId(), k -> new ArrayList<>())
                    .add(new PresentacionDto(p.getId(), p.getNombre(), p.getPrecio(), p.getRecargoExtraQueso()));
        }
        Map<Long, List<ProductoCartaDto>> porCategoria = new LinkedHashMap<>();
        for (Producto producto : productos.findByActivoTrueOrderByCategoriaIdAscNombreAsc()) {
            List<PresentacionDto> lista = porProducto.getOrDefault(producto.getId(), List.of());
            if (lista.isEmpty()) {
                continue;
            }
            porCategoria.computeIfAbsent(producto.getCategoria().getId(), k -> new ArrayList<>())
                    .add(new ProductoCartaDto(producto.getId(), producto.getNombre(), lista));
        }
        List<CategoriaCartaDto> resultado = new ArrayList<>();
        for (Categoria c : categorias.findAll(Sort.by("id"))) {
            List<ProductoCartaDto> lista = porCategoria.get(c.getId());
            if (lista != null) {
                resultado.add(new CategoriaCartaDto(c.getId(), c.getNombre(), c.getRecargoParaLlevar(), lista));
            }
        }
        return new CartaDto(resultado);
    }
}
