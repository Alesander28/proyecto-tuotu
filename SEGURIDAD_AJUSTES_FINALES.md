# Ajustes finales de seguridad – TÚ O TÚ V4

## Cambios incluidos

1. El inicio de sesión cuenta los fallos tanto por combinación de usuario + IP como por IP. Se bloquea tras 3 fallos para la misma cuenta desde esa IP y tras 15 fallos acumulados desde una IP, para reducir ataques sin bloquear toda una red compartida por solo tres errores.
2. Los intentos con un usuario inexistente también consumen el límite y ejecutan una verificación BCrypt ficticia para reducir diferencias de tiempo que puedan revelar si una cuenta existe.
3. El bloqueo por cuenta sigue siendo de 3 fallos durante 15 minutos; el límite agregado por IP es de 15 fallos durante 15 minutos. Un inicio de sesión correcto limpia los contadores correspondientes.
4. Se eliminan periódicamente contadores vencidos para evitar que el mapa en memoria crezca indefinidamente.
5. La validación de creación de usuarios ahora indica entre 12 y 72 caracteres, alineada con la regla del servicio.
6. El servicio valida también el tamaño UTF-8: BCrypt admite como máximo 72 bytes. Esto evita truncamientos o errores con contraseñas que usan caracteres multibyte.
7. El endpoint de login limita el tamaño de usuario y contraseña recibidos.
8. Se agregaron pruebas para el límite por IP con distintos nombres de usuario y para el máximo de bytes de BCrypt.

## Límite importante que permanece

El contador de intentos reside en memoria. Por ello, se reinicia al reiniciar la aplicación y no se comparte entre varias instancias. Para producción con varias instancias, mover el contador a Redis o a una tabla compartida y aplicar también rate limiting en el proxy/WAF.

La IP utilizada es `HttpServletRequest.getRemoteAddr()`. Si el sistema está detrás de un proxy inverso, configurar el proxy para que sobrescriba de forma segura la dirección remota y bloquear el acceso directo al puerto de la aplicación. No confiar ciegamente en `X-Forwarded-For` enviado por el navegador.

## Pruebas

Se agregaron/actualizaron pruebas unitarias. No se declara que la suite haya pasado: se debe ejecutar `mvnw.cmd test` (Windows, desde `backend`) y `npm test` / `npm run build` (desde `frontend`) en el entorno del proyecto.
