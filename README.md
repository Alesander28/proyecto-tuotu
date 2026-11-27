# Sistema de gestión del restaurante Tú o Tú

Sistema web para la gestión de las operaciones de un restaurante. El proyecto integra un frontend desarrollado con React y un backend REST con Spring Boot y PostgreSQL.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 3.3.5 |
| Persistencia | Spring Data JPA, Hibernate |
| Base de datos | PostgreSQL 16 |
| Frontend | React 18, Vite |
| Seguridad | Spring Security y autenticación mediante tokens |
| Contenedores | Docker y Docker Compose |
| Pruebas frontend | Vitest y Testing Library |
| Pruebas backend | JUnit y Spring Boot Test |
| Publicación frontend | GitHub Pages y GitHub Actions |

## Módulos del sistema

El código del proyecto está organizado por módulos funcionales, entre ellos:

- Seguridad y autenticación.
- Pedidos.
- Inventario.
- Ventas.
- Reportes.
- Gestión de mesas.
- Gestión de cajas.
- Auditoría.
- Funcionalidades comunes.

La disponibilidad de cada operación depende del rol y los permisos configurados en el sistema.

## Patrones de diseño

El sistema aplica patrones de diseño para separar responsabilidades y facilitar el mantenimiento:

| Patrón | Aplicación en el sistema |
|---|---|
| Strategy | Permite gestionar los recargos de los pedidos, como extra queso y para llevar, mediante estrategias independientes. |
| Adapter | Desacopla la generación de comprobantes PDF del servicio de ventas mediante una interfaz y un adaptador para OpenPDF. |
| Observer | Permite publicar eventos de pedidos y ventas para que otros componentes, como la auditoría, reaccionen sin acoplarse directamente a los servicios. |

## Roles del sistema

El acceso a las operaciones se controla mediante roles y permisos:

| Rol | Funciones principales |
|---|---|
| GERENTE | Gestiona usuarios, inventario, mesas, consulta reportes y administra operaciones autorizadas. |
| CAJERO | Consulta pedidos y registra ventas y cobros. |
| MOZO | Registra pedidos, consulta pedidos y actualiza sus estados según los permisos asignados. |

Los usuarios son gestionados por el gerente. El sistema no ofrece autorregistro público.

## Requisitos

Para ejecutar el proyecto localmente necesitas:

- Git.
- Java JDK 17.
- Node.js 20 o compatible.
- npm.
- Docker Desktop con Docker Compose.

El repositorio incluye Maven Wrapper (`mvnw.cmd`), por lo que no necesitas instalar Maven globalmente.

## Configuración inicial

1. Clona el repositorio:

   ```powershell
   git clone https://github.com/Alesander28/proyecto-tuotu.git
   cd proyecto-tuotu
   ```

2. Crea el archivo local de variables de entorno:

   ```powershell
   Copy-Item .env.example .env
   ```

3. Edita `.env` y configura tus propios valores para:

   - `DB_PASSWORD`
   - `ADMIN_USUARIO`
   - `ADMIN_CONTRASENA`
   - `CORS_ALLOWED_ORIGINS`

   La contraseña del administrador debe tener entre 12 y 72 caracteres. No compartas ni subas el archivo `.env`.

## Ejecutar con Docker

Desde la carpeta raíz del proyecto:

```powershell
docker compose up -d --build
```

Comprueba el estado de los servicios:

```powershell
docker compose ps
```

Servicios locales:

| Servicio | Dirección |
|---|---|
| Frontend de desarrollo | http://localhost:5173 |
| Backend REST | http://localhost:8080 |
| PostgreSQL desde Windows | localhost:5433 |
| PostgreSQL desde el backend Docker | postgres:5432 |

El backend espera a que PostgreSQL esté saludable antes de iniciar.

## Ejecutar el frontend

Si deseas iniciar React desde una terminal independiente:

```powershell
cd frontend
npm ci
npm run dev
```

Vite utiliza el proxy configurado para enviar las peticiones `/api` al backend en `http://localhost:8080`.

## Ejecutar pruebas

Backend:

```powershell
.\mvnw.cmd -f backend\pom.xml test
```

Frontend:

```powershell
cd frontend
npm ci
npm test
```

Compilar el frontend:

```powershell
cd frontend
npm run build
```

## Publicación

El frontend se publica mediante GitHub Pages:

https://alesander28.github.io/proyecto-tuotu/

El flujo de GitHub Actions compila React y publica el contenido de `frontend/dist` cuando se actualiza la rama `main`.

**Importante:** GitHub Pages aloja únicamente archivos estáticos. No despliega el backend Spring Boot ni la base de datos PostgreSQL. Para que el sitio publicado funcione conectado al sistema se necesita desplegar también el backend y configurar la conexión API correspondiente.

## Estructura del repositorio

```text
tuotu/
├── .github/
│   └── workflows/
├── .mvn/
├── backend/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── documentacion/
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
├── .env.example
├── .gitignore
├── docker-compose.yml
├── GITFLOW.md
├── mvnw
├── mvnw.cmd
└── README.md
```

## Documentación adicional

- [GitFlow](GITFLOW.md): estrategia de ramas, commits y versiones.
- [Documentación histórica y técnica](documentacion/): registros de cambios, seguridad y evolución del proyecto.

## Seguridad y respaldos

- No subas `.env`, contraseñas, tokens ni claves privadas.
- No subas archivos `.bak`, volcados de PostgreSQL ni respaldos locales.
- Antes de realizar operaciones destructivas en la base de datos, genera y verifica un respaldo.
- No utilices `docker compose down -v` para reiniciar servicios si deseas conservar los datos del volumen de PostgreSQL.
