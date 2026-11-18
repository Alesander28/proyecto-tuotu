# Seguridad – Fase 4: protección de recursos por propietario

## Cambios

1. **Comprobantes PDF (IDOR):** el endpoint `GET /api/comprobantes/{id}/pdf` ahora compara el usuario autenticado con el cajero que registró la venta. Si otro cajero intenta cambiar el ID y descargar el PDF, recibe `404` sin revelar si el comprobante pertenece a otra persona.
2. **Denegación por defecto de API:** las rutas `/api/**` que no estén declaradas explícitamente en `SecurityConfig` ahora se rechazan. Se mantienen explícitas las rutas de login, logout y perfil, además de los permisos ya definidos para los módulos.
3. **Prueba de integración:** se añadió un caso donde un segundo cajero intenta descargar la boleta del primero y debe recibir `404`.

## Prueba manual recomendada

- Inicia sesión como Cajero A, abre caja, cobra un pedido y descarga su boleta: debe funcionar.
- Copia el ID de esa boleta e inicia sesión como Cajero B. Intenta abrir `/api/comprobantes/{id}/pdf`: debe responder `404`.
- Verifica login, logout, `/api/auth/me` y los módulos de Gerente, Mozo y Cajero para confirmar que ninguna ruta legítima quedó bloqueada.

## Alcance y limitaciones

- La lista de pedidos sigue siendo compartida entre los roles que tienen `PEDIDOS_VER`, porque el flujo del restaurante requiere que el personal autorizado vea el estado de los pedidos. No se restringe por mozo sin confirmar primero una regla operativa distinta.
- La consulta de ventas recientes sigue dependiendo del permiso `VENTAS_REGISTRAR`; la descarga del PDF sí queda limitada al cajero propietario.
- El proyecto ZIP se valida por integridad. No se declara compilación ni ejecución de pruebas Maven: este entorno no dispone de Maven ni de Maven Wrapper en el proyecto.
