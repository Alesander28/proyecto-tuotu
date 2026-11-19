# Gitflow - Proyecto TU O TU

## Ramas principales

- main: contiene las versiones estables del sistema.
- develop: integra las nuevas funcionalidades.

## Ramas auxiliares

- feature/: desarrolla nuevas funcionalidades.
- release/: prepara una nueva version para produccion.
- hotfix/: corrige errores urgentes de produccion.

## Flujo de trabajo

1. Crear una rama feature desde develop.
2. Desarrollar y probar los cambios.
3. Crear un commit.
4. Publicar la rama en GitHub.
5. Crear un Pull Request hacia develop.
6. Integrar los cambios despues de revisarlos.
7. Crear una rama release cuando se prepare una version.
8. Integrar release en main y develop.
9. Crear hotfix desde main cuando exista un error urgente.
