# Rediseño de módulos del Mozo

## Cambios visuales
- Inicio: se mantiene el panel de resumen con pedidos reales y accesos a Mesas, Nuevo pedido y Pedidos.
- Mesas: tarjetas y estados visuales refinados, manteniendo consulta, filtros, búsqueda, historial y enlaces de pedidos.
- Nuevo pedido: carta por categorías, búsqueda de productos, botones de presentación, carrito, selección de mesa, cantidades y modificadores.
- Pedidos: tablero por estado, indicadores, búsqueda local, actualización manual/automática y botones de transición de estado.

## Funcionalidad preservada
- No se cambiaron endpoints ni DTOs.
- Nuevo pedido continúa consultando `/carta` y `/mesas` y registrando mediante `POST /pedidos`.
- Pedidos continúa consultando `/pedidos`, actualizando con `PATCH /pedidos/{id}/estado` y respetando el permiso `PEDIDOS_ESTADO`.
- Mesas conserva consulta `/mesas`, refresco automático e historial `/mesas/{id}/historial`.

## Validación
El ZIP se valida estructuralmente. La compilación Vite y las pruebas Vitest no pudieron ejecutarse porque las dependencias no estaban instaladas; `npm ci` no terminó dentro del tiempo disponible.
