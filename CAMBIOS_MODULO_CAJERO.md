# Cambios del módulo CAJERO

Se actualizó `frontend/src/modulos/ventas/Caja.jsx` para incorporar en una sola pantalla:

- Consulta del estado de la caja propia mediante `GET /api/cajas/actual`.
- Formulario de apertura de turno mediante `POST /api/cajas/abrir`.
- Resumen del turno: monto inicial, ventas registradas, efectivo esperado y ventas en efectivo.
- Lista de pedidos atendidos pendientes de cobro.
- Detalle del pedido, selección del método de pago y registro de venta/boleta.
- Formulario de cierre y arqueo mediante `POST /api/cajas/cerrar`.
- Resumen del cierre con efectivo esperado, contado y diferencia.
- Historial de ventas recientes y acceso al PDF de la boleta.

Se añadieron estilos responsivos en `frontend/src/modulos/ventas/ventas.css` y se actualizó el mock de la caja actual en `Caja.test.jsx`.

## Cómo aplicar

1. Haz una copia de seguridad de tu proyecto actual.
2. Reemplaza el archivo `frontend/src/modulos/ventas/Caja.jsx` por el de este ZIP.
3. Agrega los estilos nuevos de `frontend/src/modulos/ventas/ventas.css` (si reemplazas el archivo completo, conserva también sus reglas anteriores).
4. Actualiza `frontend/src/modulos/ventas/Caja.test.jsx` con el archivo incluido.
5. Desde `frontend`, ejecuta `npm install`, `npm test` y `npm run build`.

## Importante sobre seguridad

La interfaz bloquea el flujo normal de cobro cuando no hay una caja abierta, y los endpoints de apertura/cierre ya están protegidos con `CAJAS_OPERAR`. Sin embargo, en esta versión del backend `VentaService.registrar` todavía no exige una caja abierta al recibir directamente `POST /api/ventas`. Para seguridad financiera completa, conviene añadir esa validación en el servicio y adaptar las pruebas de integración antes de desplegarlo.

## Panel de inicio del cajero

Se añadió `frontend/src/modulos/ventas/InicioCajero.jsx` y sus estilos en `ventas.css`. El inicio del rol CAJERO ahora muestra el estado de su caja, pedidos atendidos pendientes de cobro, ventas consultadas del día, ventas del turno, accesos rápidos e historial reciente. El panel consulta los endpoints existentes y se actualiza automáticamente cada 30 segundos. La opción `Caja` del menú continúa abriendo el módulo operativo independiente.

`frontend/src/comun/Inicio.jsx` selecciona el panel según el rol/permisos: gerente, mozo o cajero.

## Verificación de esta entrega

No se pudo completar `npm run build` en este entorno: la instalación de dependencias (`npm ci`) agotó el tiempo disponible y Vite no quedó instalado. Ejecuta en tu equipo `npm ci`, `npm test` y `npm run build` desde la carpeta `frontend` antes de integrar o desplegar.
