# TiendaOnline

Proyecto Spring Boot que acompaña al [Manual TiendaOnline](https://domgarlor.github.io/manual-tienda-online/): una tienda online sencilla con catálogo, clientes, pedidos y autenticación con JWT. Usa el mismo dominio y los mismos paquetes que el curso (`catalogo`, `clientes`, `pedidos`, `comun`, `seguridad`).

## Requisitos

- JDK 17+
- Maven (o usa tu propia instalación; no incluye wrapper `mvnw`)

## Abrir en Eclipse

1. `File > Import... > Maven > Existing Maven Projects` y selecciona esta carpeta.
2. Si Eclipse usa Java 11 por defecto: clic derecho en el proyecto → `Properties > Java Build Path > Libraries`, o `Window > Preferences > Java > Installed JREs`, y asegúrate de que apunte a un JDK 17.
3. Ejecutar: clic derecho en `TiendaOnlineApplication.java` → `Run As > Java Application`.

Desde terminal, sin Eclipse:

```bash
mvn spring-boot:run
```

La app arranca en el perfil `dev` (base de datos H2 en memoria) en `http://localhost:8080`.

## Usuarios de prueba (sembrados al arrancar)

| username | password  | rol     | cliente asociado |
|----------|-----------|---------|-------------------|
| `admin`  | admin123  | ADMIN   | ninguno           |
| `ana`    | ana123    | CLIENTE | Ana Torres (id 1) |
| `luis`   | luis123   | CLIENTE | Luis Fernández (id 2) |

También hay 6 productos de catálogo precargados.

## Seguridad: JWT

- `POST /api/auth/login` y `POST /api/auth/registro` son públicos y devuelven un JWT.
- El resto de rutas van en la cabecera `Authorization: Bearer <token>`.
- El token expira a los 60 minutos (`tienda.jwt.expiracion-minutos` en `application.yml`). En producción, sobreescribe `tienda.jwt.secret` con la variable de entorno `TIENDA_JWT_SECRET`.

Reglas de acceso (`SecurityConfig`):

| Ruta                          | Acceso                    |
|-------------------------------|---------------------------|
| `POST /api/auth/**`           | público                   |
| `GET /api/productos/**`       | público                   |
| `POST /api/productos`         | solo `ADMIN`              |
| `/api/clientes/**`            | solo `ADMIN`              |
| `GET /api/pedidos/cliente/**` | solo `ADMIN`              |
| `POST /api/pedidos`, `GET /api/pedidos/mios`, `GET /api/pedidos/{id}` | cualquier usuario autenticado (un pedido ajeno da 403) |

## Ejemplos con curl

```bash
# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"ana","password":"ana123"}'
# -> { "token": "...", "username": "ana", "rol": "CLIENTE", "clienteId": 1 }

TOKEN="pega-aquí-el-token"

# Catálogo (público)
curl http://localhost:8080/api/productos

# Crear un pedido (el clienteId se deduce del token, no se envía)
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"lineas":[{"productoId":1,"cantidad":2}]}'

# Mis pedidos
curl http://localhost:8080/api/pedidos/mios -H "Authorization: Bearer $TOKEN"

# Registro de un cliente nuevo (auto-login, devuelve token)
curl -X POST http://localhost:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"username":"marta","password":"marta123","nombre":"Marta Ruiz","email":"marta@example.com"}'
```

## Consola H2

`http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:tienda`, usuario `sa`, sin contraseña.

## Perfil `prod`

`application-prod.yml` es una referencia (capítulo 6 del manual). Para usarlo de verdad hace falta descomentar la dependencia de `postgresql` en `pom.xml` y arrancar con `-Dspring.profiles.active=prod`.

## Tests

```bash
mvn test
```

Incluye un test de la capa web (`@WebMvcTest`) para `ProductoController`.
