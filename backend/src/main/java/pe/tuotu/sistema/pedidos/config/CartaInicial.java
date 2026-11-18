package pe.tuotu.sistema.pedidos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.dominio.Categoria;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import pe.tuotu.sistema.pedidos.repositorio.CategoriaRepository;
import pe.tuotu.sistema.pedidos.repositorio.PresentacionRepository;
import pe.tuotu.sistema.pedidos.repositorio.ProductoRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carga la carta real del restaurante Tú o Tú (precios en soles) la primera vez que arranca el sistema.
 * Después, el gerente la administra desde la pantalla «Productos» (HU-010).
 *
 * <p>Los cargos de la carta son: para llevar, S/ 2.00 en pizzas y S/ 1.50 en pastas y parrillas (las demás
 * categorías no cobran empaque); y extra queso en pizzas, S/ 3.00 en personal y S/ 5.00 en familiar.
 * La carta impresa indica «S/ 3.00 y S/ 5.00» sin precisar a qué tamaño corresponde cada monto; aquí se asignó
 * el menor al personal y el mayor al familiar. Si no es así, se corrige desde la administración de productos.
 */
@Component
@Order(2)
public class CartaInicial implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CartaInicial.class);

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final PresentacionRepository presentaciones;

    public CartaInicial(CategoriaRepository categorias, ProductoRepository productos,
                        PresentacionRepository presentaciones) {
        this.categorias = categorias;
        this.productos = productos;
        this.presentaciones = presentaciones;
    }

    /** Una presentación de un producto: su nombre, su precio y, si admite extra queso, el cargo. */
    private record Oferta(String nombre, String precio, String recargoExtraQueso) {
        static Oferta de(String nombre, String precio) {
            return new Oferta(nombre, precio, null);
        }

        static Oferta conQueso(String nombre, String precio, String recargoExtraQueso) {
            return new Oferta(nombre, precio, recargoExtraQueso);
        }
    }

    @Override
    public void run(String... args) {
        if (categorias.count() > 0) {
            return;
        }
        final Oferta personal = Oferta.conQueso("Personal", "22.00", "3.00");
        final Oferta familiar = Oferta.conQueso("Familiar", "42.00", "5.00");
        final Oferta personalEsp = Oferta.conQueso("Personal", "27.00", "3.00");
        final Oferta familiarEsp = Oferta.conQueso("Familiar", "47.00", "5.00");

        // ------------------------------------------------------------------ Pizzas
        Categoria pizzas = categoria("Pizzas", "2.00");
        for (String nombre : List.of("Pizza Tú o Tú", "Haz tu propia pizza", "Trabajador", "Todas las carnes", "Hawaiana",
                "Peperoni", "Tropical", "Continental", "Americana", "Española", "Alemana", "Italiana", "Napolitana",
                "Vegetariana")) {
            producto(pizzas, nombre, personal, familiar);
        }
        Categoria especiales = categoria("Pizzas especiales", "2.00");
        for (String nombre : List.of("Pizza Frutaris (dulce)", "Pizza Tingalesa", "Pizza Bolognesa", "Pizza Arcoíris",
                "Pizza Caprichosa Ele", "Pizza Blanca", "Pizza Tres Quesos", "Pizza al Pesto", "Pizza Texana",
                "Pizza a la Diabla", "Pizza Campesina", "Pizza Pechuga a la Hawaiana")) {
            producto(especiales, nombre, personalEsp, familiarEsp);
        }
        for (String nombre : List.of("Pizza Escabechada", "Pizza al Lomo Saltado", "Pizza al Ají de Gallina",
                "Pizza Pechuga a la Huancaína")) {
            producto(especiales, nombre, familiarEsp);
        }

        // ------------------------------------------------------------------ Pastas
        Categoria pastas = categoria("Pastas", "1.50");
        producto(pastas, "Lasagna Tú o Tú", unico("30.00"));
        producto(pastas, "Lasagna en salsa de carne", unico("25.00"));
        producto(pastas, "Fetuchini Tú o Tú", unico("25.00"));
        producto(pastas, "Fetuchini en salsa de carne", unico("20.00"));
        producto(pastas, "Fetuchini a lo Alfredo", unico("20.00"));
        producto(pastas, "Fetuchini a lo Alfredo con pechuga", unico("30.00"));
        producto(pastas, "Fetuchini al pesto", unico("25.00"));
        producto(pastas, "Fetuchini a la Huancayna", unico("25.00"));

        // ------------------------------------------------------------------ Parrillas
        Categoria parrillas = categoria("Parrillas", "1.50");
        producto(parrillas, "Pechuga a la plancha", unico("22.00"));
        producto(parrillas, "Pechuga light", unico("22.00"));
        producto(parrillas, "Pechuga a la plancha en salsa nativa", unico("23.00"));
        producto(parrillas, "Pechuga a la plancha en salsa de champiñones", unico("23.00"));
        producto(parrillas, "Pechuga hawaiana", unico("25.00"));
        producto(parrillas, "Pechuga campesina", unico("26.00"));
        producto(parrillas, "Pechuga a la diabla (picante)", unico("25.00"));
        producto(parrillas, "Pechuga en salsa blanca", unico("25.00"));
        producto(parrillas, "Pechuga texana", unico("25.00"));
        producto(parrillas, "Pechuga amazónica", unico("25.00"));
        producto(parrillas, "Pechuga rellena", unico("26.00"));
        producto(parrillas, "Brochetas de pollo", unico("20.00"));
        producto(parrillas, "Brochetas de carne", unico("30.00"));
        producto(parrillas, "Anticuchos de corazón de res", unico("25.00"));
        producto(parrillas, "Churrasco", unico("25.00"));
        producto(parrillas, "Costillas de cerdo a la BBQ", unico("25.00"));
        producto(parrillas, "Chuleta de cerdo a la BBQ", unico("25.00"));
        producto(parrillas, "Paiche a la parrilla", unico("30.00"));

        // ------------------------------------------------------------------ Alitas
        Categoria alitas = categoria("Alitas", "0.00");
        for (String nombre : List.of("Alitas BBQ", "Alitas BBQ Pasión", "Alitas BBQ Picante", "Alitas al Maracuyá",
                "Alitas al Maracuyá Picante", "Alitas Acevichadas", "Alitas con Miel y Mostaza", "Alitas Afrodisíacas",
                "Alitas Tingalezas")) {
            producto(alitas, nombre, Oferta.de("6 alitas", "18.00"), Oferta.de("12 alitas", "34.00"), Oferta.de("24 alitas", "55.00"));
        }

        // ------------------------------------------------------------------ Fast food
        Categoria broster = categoria("Broster", "0.00");
        producto(broster, "Pollo Broster Clásico", unico("12.00"));
        producto(broster, "Pollo Broster c/Chaufa", unico("12.00"));
        producto(broster, "Salchibroster", unico("17.00"));
        producto(broster, "Salchibroster Especial", unico("19.00"));
        producto(broster, "Pollo Broster c/n Patacones", unico("13.00"));
        producto(broster, "Yuqui Broster", unico("13.00"));

        Categoria salchichas = categoria("Salchichas", "0.00");
        producto(salchichas, "Salchipapas Económica", unico("8.00"));
        producto(salchichas, "Salchipapas Económica Especial", unico("10.00"));
        producto(salchichas, "Salchipapas Simple", unico("9.00"));
        producto(salchichas, "Salchipapa Especial", unico("12.00"));
        producto(salchichas, "Salchipapa Amazónica", unico("18.00"));
        producto(salchichas, "Salchi Plátano", unico("10.00"));
        producto(salchichas, "Salchi Yuqui", unico("10.00"));
        producto(salchichas, "Choripapas", unico("12.00"));
        producto(salchichas, "Chori Plátano", unico("12.00"));
        producto(salchichas, "Yuquitas Tingalezas", unico("15.00"));

        Categoria chaufas = categoria("Chaufas", "0.00");
        producto(chaufas, "Chaufa Amazónico", unico("18.00"));
        producto(chaufas, "Chaufa de pollo", unico("15.00"));
        producto(chaufas, "Chaufa mixto", unico("20.00"));

        // ------------------------------------------------------------------ Tacos y enchiladas
        Categoria tacos = categoria("Tacos", "0.00");
        taco(tacos, "Taco de carne", "8.00", "10.00");
        taco(tacos, "Taco de pollo", "8.00", "10.00");
        taco(tacos, "Taco de carnita", "10.00", "12.00");
        taco(tacos, "Taco mixto", "12.00", "15.00");
        taco(tacos, "Taco de salchicha de ternera", "8.00", "10.00");
        taco(tacos, "Taco de chorizo", "10.00", "12.00");
        taco(tacos, "Taco vegetariano (con frijoles)", "10.00", "12.00");
        taco(tacos, "Taco vegetariano (sin frijoles)", "10.00", "12.00");
        producto(tacos, "Taco vegano", Oferta.de("Simple", "12.00"));

        Categoria enchiladas = categoria("Enchiladas", "0.00");
        producto(enchiladas, "Enchilada de carne", unico("10.00"));
        producto(enchiladas, "Enchilada de pollo", unico("10.00"));
        producto(enchiladas, "Enchilada de carnita", unico("12.00"));
        producto(enchiladas, "Enchilada mixta", unico("15.00"));
        producto(enchiladas, "Enchilada de salchicha de ternera", unico("10.00"));
        producto(enchiladas, "Enchilada de chorizo", unico("12.00"));
        producto(enchiladas, "Enchilada vegetariana", unico("12.00"));

        // ------------------------------------------------------------------ Platos típicos y guarniciones
        Categoria tipicos = categoria("Platos típicos", "0.00");
        producto(tipicos, "Patacones con chorizo", unico("20.00"));
        producto(tipicos, "Patacones con cecina", unico("20.00"));
        producto(tipicos, "Patacones regional", unico("20.00"));
        producto(tipicos, "Tacacho con cecina", unico("20.00"));
        producto(tipicos, "Tacacho con chorizo", unico("20.00"));
        producto(tipicos, "Tacacho mixto", unico("25.00"));

        Categoria guarniciones = categoria("Guarniciones", "0.00");
        for (String nombre : List.of("Yucas fritas", "Patacones", "Plátanos maduros fritos", "Arroz blanco", "Papas fritas",
                "Ensalada simple")) {
            producto(guarniciones, nombre, unico("6.00"));
        }
        producto(guarniciones, "Arroz chaufa", unico("8.00"));

        // ------------------------------------------------------------------ Bebidas
        Categoria jugos = categoria("Jugos", "0.00");
        jugo(jugos, "Jugo especial", "12.00", "15.00");
        jugo(jugos, "Jugo surtido", "10.00", "12.00");
        jugo(jugos, "Jugo de papaya", "5.00", "7.00");
        jugo(jugos, "Jugo de piña", "5.00", "7.00");
        jugo(jugos, "Jugo de fresa", "9.00", "10.00");
        jugo(jugos, "Jugo de plátano", "5.00", "7.00");
        jugo(jugos, "Jugo de arándanos", "12.00", "13.00");
        jugo(jugos, "Jugo de lúcuma", "10.00", "12.00");
        jugo(jugos, "Jugo de guanábana", "10.00", "12.00");

        Categoria calientes = categoria("Bebidas calientes", "0.00");
        producto(calientes, "Café", unico("4.00"));
        producto(calientes, "Café con leche", unico("5.00"));
        producto(calientes, "Chocolate", unico("7.00"));
        producto(calientes, "Infusiones", unico("4.00"));
        producto(calientes, "Kión y canela", unico("5.00"));
        producto(calientes, "Té Covid", unico("5.00"));
        producto(calientes, "Relax", unico("5.00"));

        Categoria frias = categoria("Bebidas frías", "0.00");
        producto(frias, "Chicha morada", Oferta.de("Media jarra", "6.00"), Oferta.de("Jarra", "12.00"));
        producto(frias, "Maracuyá", Oferta.de("Media jarra", "6.00"), Oferta.de("Jarra", "12.00"));
        producto(frias, "Gaseosa litro", unico("8.00"));
        producto(frias, "Gaseosa medio litro", unico("5.00"));
        producto(frias, "Gaseosa personal (vidrio)", unico("3.00"));
        producto(frias, "Gaseosa personal (plástico)", unico("4.00"));

        LOG.info("Se cargó la carta del restaurante: {} categorías y {} productos.", categorias.count(), productos.count());
    }

    private static Oferta unico(String precio) {
        return Oferta.de("Único", precio);
    }

    private void taco(Categoria categoria, String nombre, String simple, String conQueso) {
        producto(categoria, nombre, Oferta.de("Simple", simple), Oferta.de("Con queso", conQueso));
    }

    private void jugo(Categoria categoria, String nombre, String simple, String conLeche) {
        producto(categoria, nombre, Oferta.de("Simple", simple), Oferta.de("Con leche", conLeche));
    }

    private Categoria categoria(String nombre, String recargoParaLlevar) {
        return categorias.save(new Categoria(nombre, new BigDecimal(recargoParaLlevar)));
    }

    private void producto(Categoria categoria, String nombre, Oferta... ofertas) {
        Producto producto = productos.save(new Producto(nombre, categoria));
        for (Oferta oferta : ofertas) {
            BigDecimal recargo = oferta.recargoExtraQueso() == null ? null : new BigDecimal(oferta.recargoExtraQueso());
            presentaciones.save(new Presentacion(oferta.nombre(), new BigDecimal(oferta.precio()), producto, recargo));
        }
    }
}
