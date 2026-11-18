# Módulo de pedidos — Sprint 2

Historias: HU-007 (registrar pedidos) y HU-008 (seguimiento del estado del pedido).

| Archivo | Qué hace |
|---|---|
| `NuevoPedido.jsx` | El mozo arma el pedido desde la carta y ve el total |
| `EstadoPedidos.jsx` | Pedidos en tres columnas según su estado |
| `pedidos.css` | Estilos de este módulo |
| `NuevoPedido.test.jsx` | Pruebas de la pantalla de nuevo pedido |

### Gestión de mesas para el mozo

La pantalla `MesasMozo.jsx` permite al rol MOZO consultar el estado real de las mesas, buscar y filtrar por disponibilidad, revisar el historial y comenzar un pedido con la mesa preseleccionada. No permite crear, activar ni desactivar mesas; esas acciones permanecen en el módulo del gerente.
