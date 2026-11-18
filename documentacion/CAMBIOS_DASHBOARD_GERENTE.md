# Rediseño del Dashboard de Gerente — versión sin duplicaciones

- Se eliminó el encabezado interior “Panel del gerente” y su descripción para evitar redundancia.
- En el encabezado superior, el rol Gerente ahora aparece como “Gerente” en lugar de “Bienvenido, Gerente”. Otros roles conservan su saludo.
- El banner muestra “Grandes sabores, mejores momentos”, sin repetir saludo ni rol.
- Se simplificaron los KPI principales a cuatro: ventas de hoy, pedidos de hoy, stock bajo y pedidos pendientes.
- Se conservaron dos estados secundarios: mesas atendidas hoy y cajas abiertas.
- Se mantienen datos del servidor, filtros de periodo, gráficos, pedidos recientes, alertas de inventario y auditoría.
- Se conserva el menú sin Configuración.
- El banner utiliza una imagen gastronómica panorámica con espacio oscuro para texto.

## Archivos principales
- `frontend/src/modulos/gerente/Dashboard.jsx`
- `frontend/src/modulos/gerente/gerente.css`
- `frontend/src/comun/Panel.jsx`
- `frontend/public/banner-gerente.png`

## Validación
No se declara compilación verificada. Ejecutar `npm ci` y `npm run build` en `frontend` tras extraer el ZIP.
