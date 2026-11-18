# Seguridad TÚ O TÚ V4 — Fase 1 y 2

## Cambios
- Sesiones: vigencia de 8 horas; tokens vencidos no se autentican.
- Token: se mantiene almacenado en el navegador como bearer token; el hash del token se guarda en la BD. El almacenamiento local sigue siendo una mitigación pendiente frente a XSS.
- Login: límite de 5 fallos por usuario/IP durante una ventana de 15 minutos (memoria del proceso; se reinicia al reiniciar el backend).
- Passwords: alta y cambio requieren 12–72 caracteres. BCrypt permanece como algoritmo de hash.
- Cambio de contraseña: revoca las sesiones existentes del usuario.
- Configuración: se quitó la contraseña predeterminada del gerente. ADMIN_CONTRASENA debe configurarse antes del primer arranque si la cuenta gerente no existe.
- Compose: PostgreSQL escucha solo en localhost y requiere DB_PASSWORD.

## Configuración local
1. Copiar `.env.example` a `.env` y cambiar las credenciales.
2. Exportar las variables ADMIN_USUARIO y ADMIN_CONTRASENA al proceso que ejecuta Spring Boot.
3. Para una base existente, ejecutar `backend/src/main/resources/migraciones/2026-11-sesion-expiracion.sql` antes de desplegar; la migración invalida sesiones antiguas.
4. Las credenciales de cuentas existentes no se modifican automáticamente. Cambie la contraseña gerente desde la interfaz de Usuarios.

## Pendientes/limitaciones
- El inicio de sesión se bloquea después de **3 intentos fallidos** durante 15 minutos. El bloqueo por intentos usa memoria local: no es distribuido y se reinicia con el servidor. En producción, usar Redis o un almacenamiento compartido y limitar también por IP en proxy.
- Token en localStorage: todavía susceptible a extracción en caso de XSS. Considerar migración a cookie HttpOnly, Secure, SameSite con protección CSRF.
- Requiere HTTPS, CORS del dominio de producción, secretos administrados, logs sin credenciales y pruebas de seguridad antes de producción.
- No se ejecutaron compilaciones/test en este entorno: Maven no está instalado y frontend dependencies no están instaladas.
