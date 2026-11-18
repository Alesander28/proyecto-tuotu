# Cómo probar los ajustes finales de seguridad

> Antes de probar, conserva una copia de tu proyecto actual. Este ZIP contiene el código fuente del proyecto y los ajustes de seguridad, no una base de datos con tus datos reales.

## 1. Backend (Windows)

Abre PowerShell y ejecuta:

```powershell
cd .\backend
mvn test
```

Si tu proyecto utiliza Maven Wrapper y tienes los archivos `mvnw.cmd` y `.mvn`, puedes ejecutar:

```powershell
.\mvnw.cmd test
```

## 2. Frontend (Windows)

Abre otra ventana de PowerShell:

```powershell
cd .\frontend
npm ci
npm test
npm run build
```

## 3. Prueba manual del bloqueo de inicio de sesión

1. Inicia la aplicación con tu perfil de desarrollo habitual.
2. Intenta iniciar sesión tres veces con una contraseña incorrecta.
3. Comprueba que la tercera respuesta sea de credenciales incorrectas y que el siguiente intento quede bloqueado durante 15 minutos.
4. Repite la prueba con nombres de usuario distintos desde la misma IP: el límite agregado por IP debe bloquear el intento número 16 (después de 15 fallos).
5. Comprueba que un inicio de sesión correcto elimine el bloqueo de esa IP.
6. Comprueba que un usuario inexistente muestre el mismo mensaje de error que una contraseña incorrecta.

## 4. Prueba de contraseñas

Al crear o cambiar la contraseña de un usuario:

- Menos de 12 caracteres: debe rechazarse.
- Más de 72 caracteres: debe rechazarse.
- Una contraseña de 37 caracteres `á`: debe rechazarse porque supera 72 bytes UTF-8.
- Una contraseña de 12 a 72 caracteres que no supere 72 bytes UTF-8: debe aceptarse.

## 5. Criterio de aceptación

No des por aprobada esta entrega hasta que `mvn test`, `npm test` y `npm run build` terminen correctamente en tu equipo y hayas completado las pruebas manuales anteriores.

## Limitación conocida

El contador de intentos continúa siendo local a cada proceso de backend. Reiniciar el backend reinicia los contadores y varias instancias no comparten el bloqueo. Para un despliegue con varias instancias se debe trasladar el contador a Redis o a una tabla compartida, además de configurar rate limiting en el proxy/WAF.
