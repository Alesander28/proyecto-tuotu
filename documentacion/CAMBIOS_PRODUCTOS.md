# Rediseño del módulo Productos (Gerente)

Se actualizó el módulo React `frontend/src/modulos/inventario/Productos.jsx` y sus estilos `inventario.css` para incorporar una interfaz de administración de productos con tarjetas y vista de tabla.

## Cambios visuales
- Resumen superior: productos activos, categorías, productos inactivos y presentaciones registradas.
- Filtros en una barra compacta: búsqueda, categoría, estado.
- Selector de vista entre tarjetas y tabla.
- Tarjetas con imagen (si el producto ya tiene URL segura), categoría, nombre, ID, precios por presentación, estado y acciones.
- Si el producto no tiene imagen, se muestra un placeholder con icono gastronómico y categoría.
- Se conservaron las acciones originales: registrar producto, crear categoría, exportar CSV, información, editar, actualizar precios, activar/desactivar.
- Se conservó la conexión a API y el modelo existente; no se agregaron campos nuevos al backend.

## Ejecutar
En `frontend`:

```bash
npm install
npm run dev
```

## Verificación
No se pudo compilar en este entorno porque faltaba el ejecutable de Vite y la instalación offline no pudo recuperar una dependencia del caché. Ejecutar `npm install` en la computadora del usuario antes de probar.
