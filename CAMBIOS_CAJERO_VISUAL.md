# Rediseño visual del Cajero

Este cambio se limita al diseño visual del módulo de caja.

- Cabecera del panel de caja con jerarquía visual y etiqueta de operación.
- Banner de turno activo con estado destacado.
- KPI de turno con tarjetas refinadas y tipografía de importes.
- Distribución de pedidos pendientes y detalle del cobro más clara.
- Panel de cobro destacado, total, método de pago y botón principal.
- Formulario de arqueo y cierre mejor organizado.
- Panel de apertura de caja más claro cuando no existe turno activo.
- Historial de ventas con tabla refinada y adaptación a móviles.

Se conservaron los estados React, funciones y endpoints originales de apertura (`POST /cajas/abrir`), cierre (`POST /cajas/cerrar`), venta (`POST /ventas`), pedidos pendientes, métodos de pago, consulta de caja y descarga de boletas PDF. No se añadieron ni retiraron operaciones.

## Verificación
- ZIP estructuralmente validado.
- No se pudo ejecutar Vite build: el paquete de dependencias no contiene el ejecutable Vite (`vite: not found`).
