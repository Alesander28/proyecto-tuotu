# Corrección del módulo de Mesas – TÚ O TÚ

Se corrigieron estos archivos:

- `backend/src/main/java/pe/tuotu/sistema/seguridad/config/DatosIniciales.java`
  - La sincronización de permisos y roles ahora se ejecuta dentro de una transacción.
  - Al iniciar, agrega al rol GERENTE los permisos que le falten, incluido `MESAS_GESTIONAR`, sin duplicar permisos por código.
- `backend/src/main/java/pe/tuotu/sistema/seguridad/config/SecurityConfig.java`
  - Se declararon expresamente los permisos para `POST /api/mesas` y `PATCH /api/mesas/{id}/activa`.
  - La consulta de mesas sigue permitida al gerente y al mozo; administrar mesas sigue reservado al gerente.
- `frontend/src/comun/Panel.jsx`
  - El indicador de conexión ahora comprueba `/api/auth/me`.
  - Un error al cargar el resumen de reportes ya no se muestra incorrectamente como “Sin conexión”.

## Cómo aplicar

1. Haz una copia de seguridad de tu proyecto y de la base de datos.
2. Extrae este ZIP en una carpeta nueva o copia los archivos corregidos respetando sus rutas.
3. Reinicia completamente el backend Spring Boot.
4. Cierra sesión en el navegador e inicia sesión nuevamente como GERENTE para renovar los permisos guardados en la sesión del frontend.
5. Entra a Mesas e intenta agregar una mesa de prueba, por ejemplo `M01`.

No se modificó ni se eliminó ninguna tabla o dato de la base de datos. No se cambió el permiso del MOZO para administrar mesas.

## Si todavía aparece 403

Comprueba en PostgreSQL que el permiso está relacionado con el rol GERENTE:

```sql
SELECT r.nombre AS rol, p.codigo AS permiso
FROM rol r
JOIN rol_permiso rp ON rp.id_rol = r.id_rol
JOIN permiso p ON p.id_permiso = rp.id_permiso
WHERE r.nombre = 'GERENTE'
ORDER BY p.codigo;
```

Debe aparecer `MESAS_GESTIONAR`. Si no aparece, comparte el resultado de esta consulta y el log del backend al iniciar para revisar el esquema real de la base de datos.
