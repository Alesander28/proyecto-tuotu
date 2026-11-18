# Seguridad Fase 5 — Configuración y preparación para producción

## Cambios incluidos
- La contraseña de PostgreSQL ya no tiene un valor predeterminado en `application.properties`: se exige `DB_PASSWORD`.
- CORS se configura mediante `CORS_ALLOWED_ORIGINS`; en desarrollo conserva `http://localhost:5173`.
- Se agrego `application-prod.properties`: exige URL, usuario, contraseña y origen CORS; usa `ddl-auto=validate` y desactiva SQL visible.
- `.gitignore` excluye archivos `.env`, claves privadas y carpetas de secretos. `.env.example` contiene solo marcadores de posición.
- Se mantiene `application-dev.properties` para H2 local. No usar el perfil `dev` en producción.

## Ejecutar localmente
Para el perfil `dev` (H2), continúa iniciando Spring Boot con `dev` activo como hasta ahora. Las credenciales de prueba de H2 son temporales.

Para ejecutar sin H2/PostgreSQL local, configura `DB_PASSWORD` en el entorno antes de iniciar el backend. No escribas la contraseña real en Git ni en archivos compartidos.

## Preparar producción
1. Crear una base PostgreSQL y un usuario dedicado con privilegios mínimos.
2. Configurar `SPRING_PROFILES_ACTIVE=prod`.
3. Inyectar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_USUARIO`, `ADMIN_CONTRASENA` y `CORS_ALLOWED_ORIGINS` desde el gestor de secretos del servidor.
4. `CORS_ALLOWED_ORIGINS` debe contener el origen HTTPS exacto del frontend (por ejemplo, `https://app.midominio.com`), sin `*`.
5. Aplicar las migraciones SQL de forma controlada antes del despliegue. `ddl-auto=validate` no crea ni actualiza tablas: solo valida el esquema existente.
6. Publicar el backend detrás de un proxy con HTTPS obligatorio. No exponer PostgreSQL a Internet; permitir conexiones solo desde el backend.
7. Verificar backups, restauración, actualizaciones de dependencias y rotación de secretos.
8. Confirmar que el usuario inicial del gerente tenga una contraseña fuerte y única. Cambiar la credencial de prueba si alguna vez se utilizó fuera de H2.

## Importante
Esta fase prepara configuración segura, pero no convierte por sí sola el sistema en un despliegue de producción certificado. Antes de publicar se debe probar el esquema real con `ddl-auto=validate`, revisar las migraciones completas, HTTPS/proxy, firewall, copias de seguridad y monitoreo. Las pruebas de Maven y frontend deben ejecutarse en el equipo de desarrollo antes de desplegar.
