# SuperApp

Ecosistema digital en una sola plataforma: una cuenta para **comprar, vender, pedir entregas y pagar**, con un historial compartido entre módulos.
Proyecto de **Ingeniería de Software I** (Universidad Nacional de Colombia), Increment 1.

- **Backend:** Spring Boot 4.1 · Java 21 · Spring Security (BCrypt + JWT) · JPA/Hibernate · PostgreSQL 16 · Flyway
- **Frontend:** aplicación web para celular (`index.html`), servida por el mismo backend
- **Arquitectura:** monolito modular por capas `Controller → Service → Repository`, con DTOs

## Cómo correrlo (un solo comando)

Requisito: **Docker** (Docker Desktop, o Colima en macOS).

```bash
git clone https://github.com/D13G0-G0/SuperApp.git
cd SuperApp
docker compose up --build
```

Abre **http://localhost:8080**. La primera vez tarda unos minutos porque descarga las dependencias y compila.
Para apagarlo: `Ctrl + C`. Para borrar también la base de datos: `docker compose down -v`.

> `docker compose` levanta PostgreSQL 16 (con healthcheck) y la app, que espera a que la base esté lista.
> Flyway crea las tablas y carga los datos de prueba solo. Las credenciales del `docker-compose.yml` son **únicamente de desarrollo**.

### Cuentas de prueba

Todas tienen la contraseña **`Demo1234`**.

| Rol | Correo | Qué puede hacer |
|---|---|---|
| Administrador | `admin@superapp.test` | Rutas `/api/admin/**` y ver cualquier usuario |
| Cliente | `cliente@superapp.test` | Buscar, pedir y pagar |
| Cliente + Vendedor | `vendedor@superapp.test` | Lo del cliente, y además publicar, editar y eliminar (tiene 6 publicaciones) |
| Conductor | `conductor@superapp.test` | Ver pedidos disponibles, aceptarlos y entregarlos |

También puedes crear una cuenta nueva desde la pantalla de inicio de sesión. Nadie puede registrarse como administrador.

## Correr las pruebas

Con **Java 21** instalado (no hace falta Docker; las pruebas usan H2 en modo PostgreSQL con las mismas migraciones):

```bash
cd backend
./mvnw verify          # Windows: mvnw.cmd verify
```

Cada pull request corre estas mismas pruebas en GitHub Actions (`.github/workflows/ci.yml`). Además, CI comprueba que `docker compose up` arranca y responde.

### Correr sin Docker (opcional)

Necesitas Java 21 y un PostgreSQL 16 local con una base `superapp`:

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5432/superapp DB_USER=superapp DB_PASSWORD=superapp \
JWT_SECRET=un-secreto-de-desarrollo-de-al-menos-32-caracteres \
./mvnw spring-boot:run
```

## Módulos

| Módulo | Qué hace | Estado |
|---|---|---|
| `identity` | Registro, inicio de sesión (BCrypt + JWT de 30 min), roles, perfil y dirección | ✅ Increment 1 |
| `catalog` | Publicar, buscar, editar, eliminar (lógico si tiene pedidos), "mis publicaciones" | ✅ Increment 1 |
| `logistics` | Pedidos de entrega con conductor (pendiente → aceptado → recogido → entregado) | ✅ base |
| `wallet` | Pagos simulados (patrones Strategy + Factory) | 🚧 billetera y movimientos en curso |
| `orders` / `history` | Carrito, pago atómico, estados del pedido e historial unificado | 🚧 en curso |
| `shared` | Seguridad (`SecurityConfig`, `CurrentUser`), errores en JSON, salud | ✅ |

### Seguridad
- Las contraseñas se guardan solo como hash **BCrypt (costo 10)**.
- `POST /api/auth/login` devuelve un **JWT que vence en 30 minutos**. Se envía en `Authorization: Bearer <token>`.
- Sin token, la API responde **401**; con un rol que no corresponde, **403**. Ambos en JSON: `{"error": "..."}`.
- Todas las reglas por rol están en `backend/src/main/java/com/superapp/api/shared/security/SecurityConfig.java`.
- El secreto del JWT llega por la variable de entorno `JWT_SECRET`. Nunca se suben claves reales.

### API principal

| Método y ruta | Quién |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | público |
| `GET /api/listings?keyword=&category=`, `GET /api/listings/{id}` | público |
| `POST /api/listings`, `PUT /api/listings/{id}`, `DELETE /api/listings/{id}`, `GET /api/listings/mine` | vendedor (solo sus publicaciones) |
| `GET /api/users/me`, `PATCH /api/users/me/shipping-address` | con sesión |
| `POST /api/deliveries`, `GET /api/deliveries/mine` | cliente |
| `GET /api/deliveries/available`, `GET /api/deliveries/assigned` | conductor |
| `GET /api/health` | público |

## Estructura de carpetas

```
SuperApp/
├── docker-compose.yml            # PostgreSQL + app: docker compose up --build
├── .github/workflows/ci.yml      # pruebas en cada pull request
├── docs/                         # notas del proyecto (el Entregable 1 vive en el Drive del grupo)
└── backend/
    ├── Dockerfile                # build multi-stage (Maven → JRE)
    ├── pom.xml
    └── src/
        ├── main/java/com/superapp/api/
        │   ├── identity/         # controller · service · repository · domain · dto
        │   ├── catalog/
        │   ├── logistics/
        │   ├── wallet/
        │   └── shared/           # security · exception · controller
        ├── main/resources/
        │   ├── application.properties
        │   ├── db/migration/     # V1__…, V2__… (Flyway). A: V1–V9 · B: V10–V19
        │   └── static/index.html # frontend para celular
        └── test/                 # pruebas unitarias y de API (MockMvc)
```

## Cómo trabajamos con git
- Nadie sube cambios directo a `main`: siempre se crea una rama y un pull request, y otra persona lo aprueba.
- Commits pequeños y con mensaje claro (`feat:`, `fix:`, `test:`, `docs:`, `build:`).
- Una migración que ya está en `main` nunca se edita. Para cualquier cambio se crea una migración nueva.

## Equipo

- Diego Andrés Rodríguez Cuaran
- Santiago Andrés Durán González
- Jesús David Cáceres Fonseca
- Juan Sebastián Basabe Hernández
- Marita Paola Urteaga Cueva

Docente: Magda Lucía Mejía Torres · Ingeniería de Software I · UNAL
