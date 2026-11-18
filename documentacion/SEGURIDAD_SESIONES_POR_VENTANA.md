# Ajuste: sesiones independientes por pestaña o ventana

## Problema
El frontend guardaba `token`, `usuario` y `tokenExpiraEn` en `localStorage`. Ese almacenamiento se comparte entre todas las pestañas del mismo origen. Al iniciar sesión como cajero en otra pestaña, se reemplazaba el token del mozo y la primera ventana podía quedar sin sesión o enviar peticiones con el token equivocado.

## Solución
Las credenciales de sesión del frontend ahora se guardan en `sessionStorage`, que es independiente para cada pestaña/ventana del navegador. Se mantiene el token Bearer y la validación de sesión del backend. El cierre de sesión y los errores 401 limpian únicamente la sesión de esa pestaña.

El ajuste no cambia permisos, expiración de 8 horas, reglas de contraseña ni controles del servidor.

## Prueba manual recomendada
1. Abrir el sistema en una ventana e iniciar sesión como mozo.
2. Abrir otra ventana independiente del mismo navegador y entrar al sistema.
3. Iniciar sesión como cajero.
4. Volver a la ventana del mozo: debe seguir en su panel y poder realizar sus operaciones autorizadas.
5. Cerrar sesión en la ventana del cajero: la ventana del mozo debe continuar autenticada.
6. Cerrar sesión en la ventana del mozo: solo esa ventana debe volver al ingreso.

Nota: cada ventana/pestaña mantiene su sesión mientras permanezca abierta. Si se abre una pestaña duplicada, el navegador puede copiar inicialmente el `sessionStorage`; para iniciar otra cuenta, abrir una pestaña nueva en blanco o una ventana nueva y navegar a la dirección del sistema.
