# ReservasLab — reservas de laboratorios con microservicios

Tres microservicios Spring Boot 4 (Java 17) con arquitectura limpia, una sola base de datos PostgreSQL
(un esquema por servicio), notificaciones por correo y dos APIs externas.

| Microservicio | Puerto local | Esquema | Qué hace |
|---|---|---|---|
| `usuarios` | 8080 | `usuarios` | Registro, login, roles, activar/desactivar cuentas |
| `reservas` | 8083 | `reservas` | Laboratorios y reservas (el núcleo del proyecto) |
| `notificaciones` | 8084 | `notificaciones` | Envía correos y guarda el historial |

**Cómo se hablan:** Reservas le pregunta a Usuarios si el usuario existe y está activo, le pregunta a la API de
festivos si el día es festivo y le pide a Notificaciones que avise por correo. Notificaciones le pide a Usuarios
el correo del usuario y envía el mensaje con la API de Brevo.

## Cómo cumple cada requisito

| Requisito | Dónde está |
|---|---|
| Core específico | Reserva de laboratorios: horarios, cruces, capacidad, festivos, estados |
| Mínimo 3 microservicios | `usuarios`, `reservas`, `notificaciones` |
| Una base de datos | Un solo PostgreSQL (`reservas_lab`); cada servicio usa su esquema (`default_schema`) |
| Sistema de notificación | Microservicio `notificaciones` (correo + historial + reintentos) |
| 5 APIs distintas al CRUD por servicio | Ver tablas de endpoints (marcadas con ★) |
| Pruebas unitarias ≥ 90 % | JUnit 5 + Mockito; JaCoCo falla el `mvn verify` si baja del 90 % |
| 2 APIs externas | Nager.Date (festivos de Colombia) y Brevo (correo) |
| Desplegado | `Dockerfile` por servicio + `render.yaml` (Render) + Neon (PostgreSQL) |

## Ejecutar en local (IntelliJ)

1. En pgAdmin: `CREATE DATABASE reservas_lab;` (los esquemas se crean solos al arrancar).
2. Abre cada carpeta (`usuarios`, `reservas`, `notificaciones`) como proyecto Maven.
3. En cada `application.properties` cambia `1234` por tu contraseña de PostgreSQL
   (o define la variable de entorno `DB_PASSWORD`).
4. En `notificaciones`, pon tu clave de Brevo y tu remitente (ver "Brevo" más abajo).
5. Arranca en este orden: `UsuariosApplication`, `NotificacionesApplication`, `ReservasApplication`.

Alternativa con Docker: `docker compose up --build` (levanta PostgreSQL y los tres servicios).

## Pruebas y cobertura

En la carpeta de cada microservicio:

```
mvn verify
```

- Ejecuta las pruebas, genera el reporte y **falla si la cobertura de líneas es menor al 90 %**.
- Reporte: `target/site/jacoco/index.html` (ábrelo en el navegador; es la evidencia para la entrega).
- En IntelliJ: panel Maven → Lifecycle → `verify`.

## Endpoints

### Usuarios — `http://localhost:8080/api/usuarios`

| | Método | Ruta | Descripción |
|---|---|---|---|
| | POST | `/registrar` | Crea un usuario (201) |
| | GET | `/lista` | Lista todos |
| | GET | `/buscar/{id}` | Consulta uno |
| | PUT | `/actualizar/{id}` | Cambia nombre, correo y rol |
| | DELETE | `/eliminar/{id}` | Elimina |
| ★ | POST | `/login` | Valida correo y clave (401 si no coinciden, 403 si la cuenta está desactivada) |
| ★ | PATCH | `/{id}/clave` | Cambia la clave pidiendo la actual |
| ★ | PATCH | `/{id}/desactivar` | Desactiva la cuenta |
| ★ | PATCH | `/{id}/activar` | Reactiva la cuenta |
| ★ | GET | `/{id}/validar` | Lo usan los otros servicios: existe, está activo y su correo |
| ★ | GET | `/estadisticas` | Totales: activos, inactivos y por rol |

```json
{ "nombre": "Ana Pérez", "correo": "ana@correo.com", "clave": "clave1234", "rol": "ESTUDIANTE" }
```
Roles: `ESTUDIANTE`, `DOCENTE`, `ADMIN`. Login: `{ "correo": "ana@correo.com", "clave": "clave1234" }`.
Cambio de clave: `{ "claveActual": "clave1234", "claveNueva": "otraClave99" }`.

### Reservas — `http://localhost:8083/api`

| | Método | Ruta | Descripción |
|---|---|---|---|
| | POST | `/laboratorios/crear` | Crea un laboratorio (201) |
| | GET | `/laboratorios/lista` | Lista todos |
| | GET | `/laboratorios/buscar/{id}` | Consulta uno |
| | PUT | `/laboratorios/actualizar/{id}` | Cambia nombre, ubicación y capacidad |
| | DELETE | `/laboratorios/eliminar/{id}` | Elimina (409 si tiene reservas activas) |
| | POST | `/reservas/crear` | Crea una reserva (201) |
| | GET | `/reservas/buscar/{id}` | Consulta una reserva |
| ★ | PATCH | `/laboratorios/{id}/estado` | Mantenimiento / disponible |
| ★ | GET | `/laboratorios/{id}/disponibilidad?fecha=2026-11-20` | Agenda del día, hora por hora |
| ★ | GET | `/laboratorios/disponibles?fecha=2026-11-20&horaInicio=08:00&horaFin=10:00` | Laboratorios libres en ese horario |
| ★ | GET | `/reservas/usuario/{idUsuario}` | Historial de un usuario |
| ★ | PATCH | `/reservas/{id}/confirmar` | PENDIENTE → CONFIRMADA |
| ★ | PATCH | `/reservas/{id}/cancelar` | PENDIENTE o CONFIRMADA → CANCELADA |
| ★ | PATCH | `/reservas/{id}/finalizar` | CONFIRMADA → FINALIZADA |
| ★ | GET | `/reservas/festivo?fecha=2026-12-25` | Consulta la API externa de festivos |

```json
{ "nombre": "Laboratorio de Redes", "ubicacion": "Bloque B - 204", "capacidad": 20 }
```
```json
{ "estado": "MANTENIMIENTO" }
```
```json
{
  "idUsuario": 1,
  "idLaboratorio": 1,
  "fecha": "2026-11-20",
  "horaInicio": "08:00",
  "horaFin": "10:00",
  "cantidadPersonas": 5,
  "motivo": "Práctica de enrutamiento"
}
```

Reglas de una reserva: horario de 07:00 a 21:00, máximo 4 horas, fecha y hora futuras, no domingos ni festivos,
usuario existente y activo, laboratorio disponible, sin superar la capacidad y sin cruzarse con otra reserva
pendiente o confirmada.

### Notificaciones — `http://localhost:8084/api/notificaciones`

| | Método | Ruta | Descripción |
|---|---|---|---|
| | GET | `/lista` | Lista todas |
| | GET | `/buscar/{id}` | Consulta una |
| | DELETE | `/eliminar/{id}` | Elimina del historial |
| ★ | POST | `/enviar` | Envía el correo y lo registra (201; `estado` dice ENVIADA o FALLIDA) |
| ★ | POST | `/{id}/reintentar` | Reenvía una FALLIDA (máximo 3 intentos) |
| ★ | PATCH | `/{id}/leida` | Marca como leída |
| ★ | GET | `/usuario/{idUsuario}` | Historial de un usuario |
| ★ | GET | `/usuario/{idUsuario}/no-leidas` | Sin leer de un usuario |
| ★ | GET | `/fallidas` | Las que no se pudieron enviar |
| ★ | GET | `/estadisticas` | Totales por estado y por tipo |

```json
{ "idUsuario": 1, "asunto": "Prueba", "mensaje": "Correo de prueba", "tipo": "GENERAL" }
```
`destinatario` es opcional: si no va, se usa el correo registrado del usuario. Tipos: `RESERVA`,
`RECORDATORIO`, `CUENTA`, `GENERAL`.

### Códigos de respuesta

400 dato inválido · 401 credenciales incorrectas · 403 cuenta desactivada · 404 no existe ·
409 regla de negocio (horario ocupado, festivo, estado no permitido) · 503 otro servicio no respondió.

## Recorrido sugerido para la demostración

1. Registrar un usuario (con un correo real para ver llegar el mensaje).
2. Crear un laboratorio.
3. `GET /reservas/festivo?fecha=2026-12-25` → `true` (API externa 1).
4. Crear una reserva → 201 y llega el correo "Reserva registrada #1" (API externa 2).
5. Repetir la misma reserva → 409 "ya está reservado en ese horario".
6. Ver `/laboratorios/1/disponibilidad?fecha=...` → las horas reservadas salen `disponible: false`.
7. Confirmar, y luego finalizar o cancelar.
8. Desactivar el usuario e intentar reservar → 409.
9. `GET /notificaciones/usuario/1` y `/estadisticas`.

## Brevo (API de correo)

1. Crea una cuenta gratuita en https://www.brevo.com (300 correos al día, sin tarjeta).
2. **Senders, Domains & Dedicated IPs → Senders → Add a sender**: agrega el correo que usarás como remitente
   y confírmalo con el enlace que te llega.
3. **SMTP & API → API Keys → Generate a new API key**: copia la clave (empieza por `xkeysib-`). Solo se muestra una vez.
4. **Importante para Render:** tu nombre (arriba a la derecha) → **Security → Authorized IPs** → desactiva
   **"Block unknown IP addresses"**. Render cambia de IP; si esto queda activo, Brevo rechaza los envíos
   y todas las notificaciones quedan `FALLIDA` con un error 401.
5. Local: ponlas en `notificaciones/src/main/resources/application.properties`
   (`brevo.api.key` y `brevo.remitente.correo`). En Render: variables `BREVO_API_KEY` y `BREVO_REMITENTE`.

No subas la clave a GitHub (en el `application.properties` deja el valor de ejemplo y usa la variable de entorno).
Si la clave o el remitente están mal, la notificación queda `FALLIDA` con el motivo en `detalleError`,
y se puede reenviar con `/reintentar`. Con un remitente Gmail es normal que el correo llegue a **spam**.

## Despliegue (Neon + Render)

### 0. Antes de desplegar
- Corre `mvn verify` en los tres servicios en tu computador. El `Dockerfile` compila **sin** ejecutar las
  pruebas (`-DskipTests`) para que el despliegue sea más rápido, así que las pruebas se validan en local.
- Revisa que en ningún `application.properties` haya contraseñas o claves reales.

### 1. Base de datos en Neon
1. Crea una cuenta en https://neon.tech y un proyecto nuevo.
   - **Region:** AWS US East (N. Virginia), la misma región que usa `render.yaml`, para que la conexión sea rápida.
   - **Postgres version:** la que venga por defecto.
2. Entra a **Connect** (o **Connection Details**) y **desactiva "Connection pooling"** para obtener la conexión directa.
   Verás algo así:
   ```
   postgresql://neondb_owner:AbC123xyz@ep-algo-123456.us-east-1.aws.neon.tech/neondb?sslmode=require
   ```
3. De ahí salen las tres variables (iguales para los tres servicios):

   | Variable | Valor (con el ejemplo de arriba) |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://ep-algo-123456.us-east-1.aws.neon.tech/neondb?sslmode=require` |
   | `DB_USER` | `neondb_owner` |
   | `DB_PASSWORD` | `AbC123xyz` |

   Fíjate en dos cosas: se antepone `jdbc:` y se **quitan** el usuario y la clave de la URL.
4. No hace falta crear tablas ni esquemas: cada servicio los crea al arrancar. Si sale
   `schema ... does not exist`, ejecuta `db/esquemas.sql` en el **SQL Editor** de Neon.

### 2. Subir el código a GitHub
La **raíz del repositorio** debe ser el contenido de esta carpeta: `render.yaml`, `usuarios/`, `reservas/`,
`notificaciones/` deben quedar directamente arriba, no dentro de otra carpeta.

Con Git (desde la carpeta `ReservasLab`):
```
git init
git add .
git commit -m "Proyecto ReservasLab"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/reservaslab.git
git push -u origin main
```
(Antes crea el repositorio vacío en GitHub. También sirve GitHub Desktop o "Upload files" en la web.)

### 3. Servicios en Render
1. Crea la cuenta en https://render.com entrando con GitHub (así ve tus repositorios).
2. **New → Blueprint** → elige el repositorio. Render lee `render.yaml` y muestra los 3 servicios.
3. Llena las variables que pide:
   - En los tres: `DB_URL`, `DB_USER`, `DB_PASSWORD` (las de Neon).
   - `USUARIOS_URL` = `https://reservaslab-usuarios.onrender.com`
   - `NOTIFICACIONES_URL` = `https://reservaslab-notificaciones.onrender.com`
   - `BREVO_API_KEY` y `BREVO_REMITENTE`.
   - Escribe las URL **sin** `/` al final.
4. **Apply**. El primer despliegue tarda varios minutos por servicio (descarga Maven y compila).
   En cada servicio, pestaña **Logs**, debe aparecer `Tomcat started on port ...`.
5. Si el nombre ya estaba tomado, Render le agrega un sufijo (`reservaslab-usuarios-abcd.onrender.com`).
   Copia la URL real que aparece arriba en cada servicio y corrige `USUARIOS_URL` / `NOTIFICACIONES_URL`
   en **Environment** (al guardar, Render vuelve a desplegar).

Sin Blueprint también se puede: **New → Web Service** por cada carpeta, Language **Docker**,
Root Directory `usuarios` / `reservas` / `notificaciones`, Instance Type **Free** y las mismas variables.

### 4. Comprobar que quedó desplegado
En el navegador o en Postman, cambiando `localhost:puerto` por la URL de Render:

1. `GET https://reservaslab-usuarios.onrender.com/api/usuarios/lista` → `[]`
2. `GET https://reservaslab-reservas.onrender.com/api/reservas/festivo?fecha=2026-12-25` → `"festivo": true`
   (prueba la API externa de festivos).
3. `GET https://reservaslab-notificaciones.onrender.com/api/notificaciones/estadisticas` → totales en 0.
4. Haz el **recorrido de la demostración** (arriba). Si el correo de la reserva llega, los tres servicios,
   la base y las dos APIs externas están funcionando juntos.

**Plan gratuito:** cada servicio se duerme a los 15 minutos sin uso y tarda cerca de un minuto en despertar
(la primera petición puede demorar o mostrar una página de carga). Antes de la sustentación abre una URL de
cada servicio. Neon también se suspende a los 5 minutos sin uso y despierta solo; los datos no se pierden.

### Errores típicos del despliegue

| Síntoma | Causa y solución |
|---|---|
| El build falla en Render | Revisa **Logs**. Casi siempre es un error de compilación: corre `mvn verify` en local primero. |
| `password authentication failed` | `DB_USER` o `DB_PASSWORD` mal copiados de Neon. |
| `The connection attempt failed` / `SSL` | Falta `?sslmode=require` o el `jdbc:` al inicio de `DB_URL`. |
| Reservas responde 503 al crear | `USUARIOS_URL` mal escrita, o Usuarios estaba dormido: espera un minuto y repite. |
| Notificaciones quedan `FALLIDA` con 401 | Clave de Brevo errada o **"Block unknown IP addresses"** activado. |
| Notificaciones quedan `FALLIDA` con 400 | `BREVO_REMITENTE` no está verificado en Brevo. |
| El correo no llega | Revisa spam y `GET /api/notificaciones/fallidas`. |
| `Could not resolve placeholder` | Falta una variable de entorno en **Environment**. |

## Estructura de cada microservicio

```
domain/Excepciones      errores del negocio
domain/model            modelos y reglas (sin Spring)
domain/model/gateway    interfaces: lo que el dominio necesita de afuera
domain/usecase          la lógica (sin Spring)
application             @Configuration: crea los casos de uso y los clientes HTTP
infraestructure/drivers_adapters/jpa_repository       base de datos
infraestructure/drivers_adapters/external_repository  llamadas a otros servicios y APIs externas
infraestructure/mapper        conversión entre capas
infraestructure/entry_points  controladores, DTO y manejo de errores
```
