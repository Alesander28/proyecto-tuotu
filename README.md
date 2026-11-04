# Sistema de gestión del restaurante Tú o Tú — Sprints 1 a 4

Adelanto de código del proyecto de Construcción de Software I.
Tecnologías (las del informe): **Java 17 + Spring Boot 3**, **PostgreSQL**, **React**.

## Qué incluye

### Sprint 1: módulo de seguridad

| Historia | Qué hace | Dónde está |
|---|---|---|
| HU-001 | Iniciar sesión con usuario y contraseña | `AuthService`, `AuthController`, pantalla `Login.jsx` |
| HU-002 | El gerente registra usuarios (sin autorregistro) | `UsuarioService.crear`, pantalla `Usuarios.jsx` |
| HU-003 | Roles y permisos: cada usuario solo ve y usa lo de su rol | `Rol`, `Permiso`, `SecurityConfig`, menú de `Panel.jsx` |
| HU-004 | El gerente actualiza los datos de un usuario | `UsuarioService.actualizar` |
| HU-005 | El gerente desactiva un usuario; su acceso se bloquea de inmediato | `UsuarioService.desactivar` |
| HU-006 | Cerrar sesión; el token deja de valer | `AuthService.logout` |

### Sprint 2: módulo de pedidos

| Historia | Qué hace | Dónde está |
|---|---|---|
| HU-007 | El mozo registra un pedido (mesa, productos, cantidades, extra queso y para llevar); el servidor calcula el total con los precios reales y lo deja en estado *Pendiente* con su código | `PedidoService.registrar`, `PedidoController`, pantalla `NuevoPedido.jsx` |
| HU-008 | Seguimiento del estado: pendiente → en preparación → atendido (solo en ese orden) | `PedidoService.cambiarEstado`, pantalla `EstadoPedidos.jsx` |

- La **carta inicial** se carga sola al arrancar (`CartaInicial`). Los precios de las pizzas son los de la carta del
  restaurante; los de **alitas y tacos son de ejemplo** y se reemplazan en el Sprint 3 (HU-010, administración de productos).
- Los cargos adicionales se configuran en `application.properties` (`app.recargo.extra-queso` y `app.recargo.para-llevar`).
- Nuevo permiso `PEDIDOS_ESTADO` (cambiar el estado de un pedido): lo tienen el **mozo** y el **gerente**. Es una decisión
  inicial; si la cocina tendrá su propio usuario, se agrega un rol.

## Patrones de diseño aplicados

Se aplicó el procedimiento de la asignatura (problema → análisis → selección → UML → implementación → pruebas) y se
usaron solo los patrones que resuelven un problema real del sistema. El análisis completo está en la sección 16 del informe.

| Patrón | Problema que resuelve | Clases | Prueba |
|---|---|---|---|
| **Strategy** | Los cargos del pedido (extra queso, para llevar) estaban como condicionales dentro de `PedidoService`. Ahora cada cargo es una estrategia | `pedidos/servicio/recargo/`: `RecargoPedido`, `RecargoExtraQueso`, `RecargoParaLlevar` | `RecargoPedidoTest`, `PedidoServiceTest` |
| **Adapter** | La boleta estaba atada a la biblioteca OpenPDF dentro del servicio. Ahora el servicio depende del contrato `GeneradorComprobante` | `ventas/servicio/GeneradorComprobante`, `ventas/adaptador/ComprobantePdfOpenPdf` | `ComprobanteServiceTest`, `VentaApiTest` |
| **Observer** | Cada interesado nuevo en lo que pasa con un pedido (bitácora, pantalla de cocina…) obligaba a modificar los servicios. Ahora los servicios publican eventos | `pedidos/evento/`, `ventas/evento/`, `auditoria/AuditoriaOperaciones` | `EventosApiTest` |

**Cómo agregar un cargo nuevo** (por ejemplo, delivery): crear una clase `@Component` que implemente `RecargoPedido`.
`PedidoService` no cambia. **Cómo agregar un observador:** crear una clase con un método `@EventListener`.

Se evaluaron y **no** se aplicaron State (los tres estados del pedido no tienen comportamiento propio) ni una fachada
aparte (`VentaService` ya cumple ese papel). Límites: los eventos son síncronos y locales (no son mensajería distribuida);
y hoy hay un solo adaptador de PDF.

## Cómo probar todo el flujo (recorrido completo)

1. Ingrese como `gerente` y, en *Usuarios*, cree un usuario **MOZO** (por ejemplo `mozo01`) y otro **CAJERO** (`cajero01`).
2. En *Productos* revise la carta y en *Inventario* registre una entrada y una salida de un insumo.
3. Cierre sesión e ingrese como `mozo01`: en *Nuevo pedido* arme un pedido; en *Pedidos* avánzalo a «En preparación» y luego a «Atendido».
4. Cierre sesión e ingrese como `cajero01`: en *Caja* elija el pedido atendido, elija el método de pago, registre la venta y abra la boleta en PDF.
5. Ingrese de nuevo como `gerente` y abra *Reportes* para ver las ventas del día y el movimiento del inventario.

Cada usuario ve en el menú solo las opciones de su rol.

### Sprint 3: módulo de inventario (solo el gerente)

| Historia | Qué hace | Dónde está |
|---|---|---|
| HU-010 | Administra la carta: categorías, productos con sus presentaciones y precios; un producto se desactiva (no se borra) y deja de aparecer al tomar pedidos | `ProductoService`, pantalla `Productos.jsx` |
| HU-011 | Entradas de insumos (cantidad, fecha, responsable): aumentan el stock | `InsumoService.registrarEntrada` |
| HU-012 | Salidas de insumos (cantidad, fecha, motivo): disminuyen el stock; **nunca puede quedar en negativo** | `InsumoService.registrarSalida` |
| HU-013 | Consulta del inventario con buscador e historial de movimientos | pantalla `Inventario.jsx` |

Los 5 insumos que se cargan al arrancar son **de ejemplo**: reemplácelos con los reales.

### Sprint 4: módulos de ventas y de reportes

| Historia | Qué hace | Dónde está |
|---|---|---|
| HU-009 | El cajero cobra un pedido **atendido** (un pedido solo tiene una venta) con un método de pago | `VentaService`, pantalla `Caja.jsx` |
| HU-014 | La venta emite su **boleta de consumo** en PDF con numeración correlativa (B001-00000001, …). Es de control interno, sin validez tributaria | `ComprobanteService` (biblioteca OpenPDF) |
| HU-015 | El gerente ve reportes de ventas (total, por categoría, más vendidos, por método de pago) e inventario (entradas, salidas y stock) | `ReporteService`, pantalla `Reportes.jsx` |

- Los métodos de pago (`Efectivo, Yape, Plin, Tarjeta`) y la serie de la boleta (`B001`) se configuran en `application.properties`.
- El reporte se imprime o se guarda como PDF con el botón «Imprimir / guardar como PDF» (usa la impresión del navegador).
- Si Maven no encuentra la versión de OpenPDF del `pom.xml` (`1.3.30`), cámbiela por una versión 1.3.x disponible en search.maven.org.

Requerimientos no funcionales cubiertos: RNF-01 (autenticación, contraseñas con hash BCrypt),
RNF-02 (control de acceso por rol validado en el servidor), RNF-06 (nombre de usuario único, datos obligatorios).

## Cómo funciona la seguridad (resumen)

1. `POST /api/auth/login` valida usuario y contraseña (BCrypt). Si algo falla responde siempre
   «Usuario o contraseña incorrectos», sin decir cuál dato falló.
2. Si es correcto, se crea un **token aleatorio**. En la base de datos solo se guarda su hash (tabla `sesion`).
3. En cada petición, `AuthTokenFilter` busca la sesión no revocada de un usuario activo y le da los permisos de su rol.
4. Al **cerrar sesión** o **desactivar** al usuario, sus sesiones se marcan como revocadas: el token deja de funcionar al instante.

Roles y permisos iniciales (se crean solos al arrancar):

| Rol | Permisos |
|---|---|
| GERENTE | USUARIOS_GESTIONAR, INVENTARIO_GESTIONAR, REPORTES_VER, PEDIDOS_VER, PEDIDOS_ESTADO |
| CAJERO | PEDIDOS_VER, VENTAS_REGISTRAR |
| MOZO | PEDIDOS_REGISTRAR, PEDIDOS_VER, PEDIDOS_ESTADO |

Cuenta inicial: usuario **gerente**, contraseña **Gerente123\*** (cámbiela; se configura en `application.properties`
o con las variables `ADMIN_USUARIO` y `ADMIN_CONTRASENA`).

## Requisitos

- JDK 17 o superior y Maven 3.9+
- Node.js 18 o superior
- (Opcional) Docker, solo si quiere usar PostgreSQL en lugar de la base en memoria

## Cómo ejecutarlo (sin instalar PostgreSQL)

**1. Backend** (puerto 8080), con la base de datos en memoria H2:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**2. Frontend** (puerto 5173), en otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173 e ingrese con `gerente` / `Gerente123*`.
Desde «Usuarios» cree un mozo y un cajero; ingrese con ellos para comprobar que no ven la opción «Usuarios».

## Cómo ejecutarlo con PostgreSQL

```bash
docker compose up -d          # levanta PostgreSQL (base "tuotu")
cd backend
mvn spring-boot:run           # usa PostgreSQL por defecto
```

Las tablas se crean solas al arrancar. Para otra base, defina `DB_URL`, `DB_USER` y `DB_PASSWORD`.

## Pruebas

```bash
cd backend && mvn test        # 13 clases de pruebas (unitarias y de la API completa)
cd frontend && npm test       # 14 pruebas de las pantallas de los 4 módulos
```

Las pruebas del backend cubren: contraseña incorrecta (mensaje único), usuario desactivado, nombre de usuario
repetido, un mozo que recibe «acceso denegado» (403) al gestionar usuarios, y token inválido tras cerrar sesión
o desactivar al usuario.

## Endpoints

| Método y ruta | Permiso | Descripción |
|---|---|---|
| `POST /api/auth/login` | público | Inicia sesión; devuelve token y datos del usuario |
| `POST /api/auth/logout` | sesión | Cierra la sesión (revoca el token) |
| `GET /api/auth/me` | sesión | Datos del usuario actual |
| `GET /api/roles` | USUARIOS_GESTIONAR | Lista roles y permisos |
| `GET /api/usuarios` | USUARIOS_GESTIONAR | Lista usuarios |
| `POST /api/usuarios` | USUARIOS_GESTIONAR | Registra un usuario |
| `PUT /api/usuarios/{id}` | USUARIOS_GESTIONAR | Actualiza nombre, rol y (opcional) contraseña |
| `PATCH /api/usuarios/{id}/desactivar` | USUARIOS_GESTIONAR | Desactiva la cuenta y revoca sus sesiones |
| `GET /api/carta` | PEDIDOS_REGISTRAR o PEDIDOS_VER | Carta por categorías, con precios y cargos adicionales |
| `POST /api/pedidos` | PEDIDOS_REGISTRAR | Registra un pedido (devuelve su código y el total) |
| `GET /api/pedidos?estado=` | PEDIDOS_VER | Lista los últimos 100 pedidos (filtro opcional por estado) |
| `GET /api/pedidos/{id}` | PEDIDOS_VER | Detalle de un pedido |
| `GET/POST /api/productos`, `PUT /api/productos/{id}` | INVENTARIO_GESTIONAR | Administra productos (activar o desactivar) |
| `GET/POST /api/productos/categorias` | INVENTARIO_GESTIONAR | Categorías de la carta |
| `POST /api/productos/{id}/presentaciones`, `PUT /api/productos/presentaciones/{id}/precio` | INVENTARIO_GESTIONAR | Presentaciones y precios |
| `GET/POST /api/insumos` | INVENTARIO_GESTIONAR | Lista (con `?q=`) y registra insumos |
| `POST /api/insumos/{id}/entradas` y `/salidas` | INVENTARIO_GESTIONAR | Registra una entrada o una salida |
| `GET /api/insumos/{id}/movimientos` | INVENTARIO_GESTIONAR | Historial de movimientos |
| `GET /api/ventas/pendientes` | VENTAS_REGISTRAR | Pedidos atendidos por cobrar |
| `POST /api/ventas` | VENTAS_REGISTRAR | Cobra un pedido y emite su boleta |
| `GET /api/ventas`, `GET /api/ventas/metodos-pago` | VENTAS_REGISTRAR | Ventas recientes y métodos de pago |
| `GET /api/comprobantes/{id}/pdf` | VENTAS_REGISTRAR | Boleta en PDF |
| `GET /api/reportes/ventas?desde=&hasta=` | REPORTES_VER | Reporte de ventas (sin fechas: hoy) |
| `GET /api/reportes/inventario?desde=&hasta=` | REPORTES_VER | Reporte de inventario |
| `PATCH /api/pedidos/{id}/estado` | PEDIDOS_ESTADO | Pasa el pedido al estado siguiente |

Las peticiones con sesión llevan el encabezado `Authorization: Bearer <token>`.

## Estructura: un módulo por Sprint

El código está organizado **por módulos**: cada Sprint tiene su propia carpeta, y los módulos funcionan juntos
(el módulo de pedidos usa al de seguridad para saber quién es el mozo).

| Módulo | Sprint | Historias | Backend (`backend/src/main/java/pe/tuotu/sistema/`) | Frontend (`frontend/src/`) |
|---|---|---|---|---|
| **seguridad** | 1 | HU-001 a HU-006 | `seguridad/` | `modulos/seguridad/` |
| **pedidos** | 2 | HU-007 y HU-008 | `pedidos/` | `modulos/pedidos/` |
| comunes | — | — | `comun/` (errores de la API) y `auditoria/` (observadores) | `comun/` (API, menú, estilos) |
| **inventario** | 3 | HU-010 a HU-013 | `inventario/` | `modulos/inventario/` |
| **ventas** | 4 | HU-009 y HU-014 | `ventas/` | `modulos/ventas/` |
| **reportes** | 4 | HU-015 | `reportes/` | `modulos/reportes/` |

Dentro de cada módulo del backend, las carpetas siguen la arquitectura en capas del informe:

```
backend/src/main/java/pe/tuotu/sistema/
├── seguridad/            (Sprint 1)
│   ├── controlador/      API REST: AuthController, UsuarioController, RolController
│   ├── servicio/         Reglas de negocio: AuthService, UsuarioService
│   ├── repositorio/      Acceso a la base de datos
│   ├── dominio/          Entidades: Usuario, Rol, Permiso, SesionToken
│   ├── dto/              Objetos de entrada y salida de la API
│   ├── config/           Spring Security, filtro del token y datos iniciales
│   └── util/             TokenUtil
├── pedidos/              (Sprint 2)
│   ├── controlador/      CartaController, PedidoController
│   ├── servicio/         PedidoService, CartaService
│   ├── repositorio/
│   ├── dominio/          Pedido, DetallePedido, EstadoPedido, Categoria, Producto, Presentacion
│   ├── dto/
│   └── config/           CartaInicial (carta de ejemplo)
├── inventario/           (Sprint 3) insumos, entradas, salidas y administración de la carta
├── ventas/               (Sprint 4) ventas y boleta de consumo en PDF
├── reportes/             (Sprint 4) reportes de ventas e inventario
├── auditoria/            (Observer) bitácora de lo que ocurre con pedidos y ventas
├── comun/                ApiException y ManejadorErrores
└── SistemaApplication.java

backend/src/test/java/pe/tuotu/sistema/   Las pruebas están en la misma estructura: seguridad/ y pedidos/
frontend/src/
├── modulos/seguridad/    Login, Usuarios, auth.jsx (Sprint 1)
├── modulos/pedidos/      NuevoPedido, EstadoPedidos (Sprint 2)
├── modulos/inventario/   Inventario, Productos (Sprint 3)
├── modulos/ventas/       Caja (Sprint 4)
├── modulos/reportes/     Reportes (Sprint 4)
└── comun/                api.js, Panel, Inicio, estilos
```

Cada módulo tiene un archivo que explica qué contiene (`package-info.java` en el backend y `LEEME.md` en el frontend).
Hay dos archivos compartidos que reúnen reglas de los cuatro Sprints, y por eso llevan comentarios que marcan
qué línea es de cada uno: `seguridad/config/SecurityConfig.java` (rutas protegidas) y `seguridad/config/DatosIniciales.java` (permisos y roles).

Sigue la arquitectura en capas del informe (controlador → servicio → repositorio → base de datos).
