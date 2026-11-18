# Consolidación TÚ O TÚ V4

Esta entrega reúne en un solo proyecto la versión más reciente de los módulos trabajados y aprobados durante las pruebas locales.

## Módulos incluidos

- **Gerente:** inicio, productos, inventario, reportes, usuarios, mesas, cajas y auditoría.
- **Mozo:** inicio, mesas, nuevo pedido y pedidos.
- **Cajero:** inicio, pedidos, caja, cobros, boletas y arqueo/historial.

## Base de consolidación

Se tomó como base el ZIP más reciente del rediseño de Cajero. Se comparó con el ZIP del rediseño de Mozo: la versión de Cajero conserva todos los archivos de la versión de Mozo y agrega el documento de cambios visuales de Caja; las diferencias de código corresponden a `frontend/src/modulos/ventas/Caja.jsx` y `frontend/src/modulos/ventas/ventas.css`.

## Antes de ejecutar

1. Revisa `README.md` para los pasos de inicio y el recorrido funcional.
2. Instala las dependencias del frontend dentro de `frontend` con `npm install` y ejecuta `npm run dev`.
3. Inicia PostgreSQL (o usa el perfil `dev` de H2, según el README) y ejecuta el backend desde `backend` con Maven.
4. Cambia la contraseña inicial del gerente. La configuración permite definir `ADMIN_USUARIO`, `ADMIN_CONTRASENA`, `DB_URL`, `DB_USER` y `DB_PASSWORD` mediante variables de entorno.
5. Haz una copia de seguridad de la base de datos antes de reemplazar una instalación existente.

## Alcance de la verificación de esta consolidación

- Se verificó la integridad de los ZIP de origen y se comparó el contenido de las versiones de Mozo y Cajero.
- La compilación del frontend no pudo ejecutarse en este entorno porque no estaba disponible el ejecutable local de Vite; la instalación de dependencias excedió el tiempo disponible.
- No se ejecutó la compilación del backend porque Maven no está instalado en este entorno.
- Las pruebas funcionales locales de los módulos fueron realizadas y aprobadas por el usuario.

Esta consolidación no reemplaza la prueba final en el equipo donde se ejecuta PostgreSQL y el backend.
