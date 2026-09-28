# Desplegar TiendaOnline (backend + frontend + BBDD) gratis, con auto-deploy

Un único entorno: producción. Cada `git push` a `master` en cualquiera de los
dos repos (`tienda-online`, `tienda-online-web`) redespliega solo — no hace
falta ni pipeline propio ni pulsar nada.

Orden recomendado: **1) base de datos → 2) backend → 3) frontend → 4) cerrar el CORS**.
El motivo del orden: el backend necesita la URL de la BBDD antes de arrancar, y
el CORS del backend necesita la URL final del frontend, que no se conoce hasta
que este está desplegado.

## 1. Base de datos Postgres gratuita

Dos opciones válidas, elige una:

**Opción A — Neon** (recomendado: sin caducidad conocida en el plan gratis)
1. Crea cuenta en [neon.tech](https://neon.tech) con tu GitHub.
2. "Create a project" → cualquier nombre y región.
3. Copia la "Connection string" que te da. Tiene esta forma:
   `postgresql://usuario:contraseña@ep-xxxx.eu-central-1.aws.neon.tech/neondb?sslmode=require`
4. Conviértela a formato JDBC (solo cambia `postgresql://` por `jdbc:postgresql://`
   y quita el usuario/contraseña de la URL, van aparte):
   - `DB_URL` = `jdbc:postgresql://ep-xxxx.eu-central-1.aws.neon.tech/neondb?sslmode=require`
   - `DB_USER` = el usuario de la connection string
   - `DB_PASSWORD` = la contraseña de la connection string

**Opción B — Postgres del propio Render**
1. En el dashboard de Render: "New" → "PostgreSQL" → plan Free.
2. Aviso: el plan gratis de Render expira a los 90 días (hay que crear uno
   nuevo o pasar a pago). Para un proyecto de práctica es asumible, pero
   tenlo en cuenta.
3. Render te da directamente `DB_URL`, `DB_USER` y `DB_PASSWORD` en la pestaña
   "Connect" de la base de datos.

Guarda esos tres valores, los necesitas en el paso siguiente.

## 2. Backend en Render

1. Crea cuenta en [render.com](https://render.com) con tu GitHub.
2. "New" → "Blueprint" → selecciona el repo `tienda-online`. Render lee el
   `render.yaml` del repo y propone crear el servicio solo.
   - Si prefieres hacerlo a mano en vez de con el blueprint: "New" → "Web
     Service" → conecta `tienda-online` → Runtime: **Docker** (detecta el
     `Dockerfile` solo) → plan **Free**.
3. Antes de darle a crear, revisa las variables de entorno (la mayoría ya
   vienen del `render.yaml`; `TIENDA_JWT_SECRET` y `TIENDA_ADMIN_PASSWORD`
   se generan solas):

   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` (ya viene puesto) |
   | `DB_URL` | la del paso 1 |
   | `DB_USER` | la del paso 1 |
   | `DB_PASSWORD` | la del paso 1 |
   | `TIENDA_CORS_ALLOWED_ORIGINS` | de momento pon `http://localhost:5173`, lo cambiamos en el paso 4 |

4. Crea el servicio. Tarda unos minutos en construir la imagen Docker la
   primera vez. Cuando termine, Render te da una URL tipo
   `https://tienda-online-api.onrender.com` — **guárdala**, la necesitas para
   el frontend.
5. Comprueba que responde: `https://tu-url.onrender.com/api/productos` debe
   devolver el catálogo en JSON (la primera petición puede tardar ~30-60s si
   el servicio llevaba rato dormido: el plan gratis duerme tras 15 min sin
   tráfico).

A partir de aquí, cada `git push` a `master` en `tienda-online` reconstruye y
redespliega solo.

## 3. Frontend en Netlify (o Vercel)

**Netlify:**
1. Cuenta en [netlify.com](https://netlify.com) con tu GitHub.
2. "Add new site" → "Import an existing project" → selecciona
   `tienda-online-web`. El `netlify.toml` del repo ya trae el build command,
   la carpeta de publicación y la regla de rewrite para que las rutas de
   React Router no den 404 al refrescar.
3. Antes de desplegar, añade la variable de entorno:
   - `VITE_API_URL` = la URL de Render del paso 2 (sin barra al final)
4. Despliega. Netlify te da una URL tipo `https://tienda-online-web.netlify.app`.

**Vercel** es la alternativa directa: mismo flujo (import desde GitHub,
variable `VITE_API_URL`), y usa el `vercel.json` del repo para el rewrite de
rutas en vez del `netlify.toml`.

## 4. Cerrar el círculo: CORS

Ahora que conoces la URL real del frontend:

1. Vuelve al servicio de Render (backend).
2. Cambia la variable `TIENDA_CORS_ALLOWED_ORIGINS` a la URL de Netlify/Vercel
   (por ejemplo `https://tienda-online-web.netlify.app`). Si quieres seguir
   probando también en local, sepáralas por coma:
   `https://tienda-online-web.netlify.app,http://localhost:5173`
3. Guarda — Render redespliega automáticamente al cambiar una variable.

Con esto, abrir la URL del frontend ya debería mostrar el catálogo real
sirviéndose desde el backend en Render con datos persistidos en Postgres.

## Notas y límites conocidos (proyecto de práctica, no producción real)

- **Cold starts**: el plan gratis de Render duerme el backend tras 15 min sin
  tráfico. La primera petición tras dormir tarda bastante.
- **Esquema de BBDD con `ddl-auto: update`**: sin Flyway/Liquibase, Hibernate
  crea/actualiza las tablas solo en cada arranque. Válido aquí; en un
  proyecto real con datos importantes se sustituiría por migraciones
  versionadas.
- **Contraseña de admin**: Render la genera aleatoria
  (`TIENDA_ADMIN_PASSWORD`) y la puedes ver en la pestaña "Environment" del
  servicio — no queda en el código ni en GitHub.
- **CI**: `.github/workflows/ci.yml` en ambos repos ejecuta los tests
  (backend) y el build (frontend) en cada push, pero no bloquea el deploy de
  Render/Netlify si fallan — son plataformas independientes. Si quieres que
  el deploy espere a que el CI esté en verde, ese ajuste se hace desde el
  propio panel de Render ("Auto-Deploy" → requerir status checks).
