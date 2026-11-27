# GitFlow - Proyecto Tú o Tú

## 1. Ramas principales

- `main`: contiene las versiones estables del sistema. Los cambios que llegan a esta rama deben estar revisados y probados.

- `develop`: integra las funcionalidades terminadas antes de preparar una nueva versión.

## 2. Ramas auxiliares

| Rama | Propósito | Rama de origen | Integración |
|---|---|---|---|
| `feature/<nombre>` | Desarrollar funcionalidades nuevas | `develop` | `develop` |
| `release/<version>` | Preparar una versión para producción | `develop` | `main` y `develop` |
| `hotfix/<nombre>` | Corregir errores urgentes en producción | `main` | `main` y `develop` |

## 3. Desarrollo de funcionalidades

1. Actualizar la rama `develop`.

2. Crear una rama `feature/<nombre>` desde `develop`.

3. Implementar la funcionalidad.

4. Ejecutar las pruebas correspondientes.

5. Registrar los cambios mediante commits.

6. Publicar la rama en GitHub.

7. Crear un Pull Request hacia `develop`.

8. Revisar e integrar los cambios después de su aprobación.

Ejemplo:

```bash

git switch develop

git pull origin develop

git switch -c feature/nombre-funcionalidad

```

## 4. Preparación de versiones

1. Crear una rama `release/<version>` desde `develop`.

2. Realizar las correcciones y verificaciones finales.

3. Probar el sistema antes de publicar.

4. Crear un Pull Request hacia `main`.

5. Integrar la versión aprobada en `main`.

6. Integrar también los cambios de `release` en `develop`.

7. Mantener ambas ramas sincronizadas.

Ejemplo de nombre:

`release/1.0.0`

## 5. Correcciones urgentes

1. Crear una rama `hotfix/<nombre>` desde `main`.

2. Corregir el problema.

3. Ejecutar las pruebas necesarias.

4. Crear un Pull Request hacia `main`.

5. Integrar la corrección en `main`.

6. Integrar también la corrección en `develop`.

Ejemplo de nombre:

`hotfix/correccion-login`

## 6. Convenciones de trabajo

- Utilizar nombres descriptivos para las ramas.

- No desarrollar funcionalidades directamente en `main`.

- Mantener actualizadas las ramas antes de comenzar un trabajo.

- Realizar commits pequeños y relacionados con un cambio concreto.

- Probar los cambios antes de crear un Pull Request.

- Revisar los conflictos antes de integrar ramas.

- No subir contraseñas, tokens, archivos `.env` ni respaldos de bases de datos.

## 7. Publicación del frontend

El proyecto utiliza GitHub Actions para compilar y publicar el frontend en GitHub Pages.

El workflow `.github/workflows/deploy.yml` se ejecuta cuando se realizan cambios en la rama `main` o cuando se inicia manualmente.

La publicación utiliza el contenido generado en `frontend/dist`.

**Importante:** GitHub Pages publica únicamente el frontend estático. El backend Spring Boot y PostgreSQL requieren un despliegue independiente.

## 8. Esquema general

El flujo de trabajo para cada tipo de rama es:

```text
develop -> feature/nombre -> develop
develop -> release/version -> main y develop
main    -> hotfix/nombre  -> main y develop
```

Las ramas release y hotfix deben integrar sus cambios tanto en main como en develop.