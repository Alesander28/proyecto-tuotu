# Cambio: selector dinámico de mesas para el mozo

## Qué se modificó

- `frontend/src/modulos/pedidos/NuevoPedido.jsx`: ahora consulta `GET /api/mesas` además de la carta.
- El selector muestra únicamente mesas `activa = true` y `estado = LIBRE`.
- Las mesas aparecen ordenadas por número y se muestran como `Mesa N — Disponible`.
- Si no hay mesas disponibles, se informa al mozo y no se permite enviar el pedido.
- Si la pantalla recibe `?mesa=...`, preselecciona esa mesa únicamente si sigue activa y libre.
- Tras registrar correctamente un pedido, la mesa usada se retira de las opciones disponibles en esa pantalla.
- `NuevoPedido.test.jsx`: se agregaron pruebas para la carga y filtrado de mesas y para enviar el pedido con la mesa seleccionada.

## Endpoint utilizado

`GET /api/mesas`

El backend existente ya permite este GET a usuarios con `PEDIDOS_REGISTRAR` o `MESAS_GESTIONAR`. El DTO ya entrega `id`, `numero`, `activa` y `estado`, por lo que no fue necesario cambiar el backend ni la base de datos.

## Cómo probar

1. Iniciar sesión como gerente y crear/activar al menos una mesa.
2. Iniciar sesión como mozo.
3. Abrir **Nuevo pedido**.
4. En el campo **Mesa**, comprobar que aparezca la mesa creada y disponible.
5. Registrar un pedido y verificar que la mesa ya no aparezca como libre en la lista tras la actualización de la pantalla.

Si no aparece ninguna mesa, revisar que el mozo tenga el permiso `PEDIDOS_REGISTRAR` y que `GET /api/mesas` responda correctamente.
