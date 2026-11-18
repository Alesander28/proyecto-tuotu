# Rediseño del módulo Inventario — Gerente

- Se conserva la integración existente con la API `/insumos` y `/insumos/{id}/entradas`, `/salidas`, `/stock-minimo` y `/movimientos`.
- Nuevo resumen visual: insumos registrados, stock normal, por reponer y agotados.
- Buscador y filtro por estado de stock.
- Tabla rediseñada con stock actual, unidad, stock mínimo, insignia de estado y acciones.
- Acciones: registrar entrada, registrar salida, configurar stock mínimo y ver historial.
- Formulario para registrar insumos con unidad y mínimo de reposición; stock inicial continúa en cero, se carga mediante una entrada.
- Panel de movimientos recientes (últimos 30 días de la API), entradas/salidas, cantidad, fecha, usuario y motivo.
- Exportación CSV conserva la funcionalidad.
- CSS responsive para pantallas pequeñas y estilo alineado a la marca (marrón, naranja, crema).
- No se cambió el backend ni las reglas de negocio.

Nota de verificación: ZIP validado. No fue posible instalar dependencias npm en el entorno de trabajo por timeout, por lo que no se pudo ejecutar `npm run build`.
