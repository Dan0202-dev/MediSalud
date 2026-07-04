# MediSalud — Sistema de Agendamiento de Citas Médicas

API REST para el agendamiento de citas médicas de la clínica **MediSalud**. Permite
registrar médicos y pacientes, reservar y cancelar citas, reprogramarlas, consultar
franjas horarias disponibles y aplicar reglas de negocio para controlar la
disponibilidad y reducir el ausentismo.

> Prueba técnica — backend (API REST). No incluye frontend ni autenticación (fuera de alcance).

---

## Tabla de contenidos

- [Tecnologías](#tecnologías)
- [Arquitectura y justificación](#arquitectura-y-justificación)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
- [Documentación interactiva (Swagger)](#documentación-interactiva-swagger)
- [Configuración](#configuración)
- [Endpoints de la API](#endpoints-de-la-api)
- [Reglas de negocio](#reglas-de-negocio)
- [Manejo de errores](#manejo-de-errores)
- [Pruebas](#pruebas)
- [Despliegue](#despliegue)
- [Decisiones y limitaciones conocidas](#decisiones-y-limitaciones-conocidas)

---

## Tecnologías

| Categoría | Tecnología |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Framework | Spring Boot 3.5.x (Web, Data JPA, Validation, Actuator) |
| Persistencia | Spring Data JPA + Hibernate, base de datos **H2 en archivo** |
| Documentación | springdoc-openapi (Swagger UI / OpenAPI 3) |
| Utilidades | Lombok |
| Pruebas | JUnit 5, Mockito, Spring Boot Test (MockMvc) |
| Build | Maven (con wrapper `mvnw`) |

---

## Arquitectura y justificación

Se eligió una **arquitectura por capas (layered)** con una separación clara de
responsabilidades y las dependencias apuntando hacia el dominio:

```
Controller  →  Service  →  Repository  →  Base de datos
   (REST)     (negocio)   (Spring Data)      (H2)
     │            │
    DTOs      Entidades de dominio
```

- **Controller**: expone la API REST, valida el contrato de entrada (`@Valid`) y
  traduce a códigos HTTP. No contiene lógica de negocio.
- **Service**: concentra la lógica y **todas las reglas de negocio** (RN-01..RN-06).
  Es transaccional (`@Transactional`).
- **Repository**: acceso a datos con Spring Data JPA (incluye `Specification` para
  filtros dinámicos y consultas derivadas).
- **DTOs (records)**: contrato de la API desacoplado de las entidades. **Nunca se
  exponen las entidades JPA directamente** (seguridad y evolución independiente).
- **Exception**: manejo de errores centralizado con `@RestControllerAdvice`.

### ¿Por qué capas y no hexagonal?

La arquitectura hexagonal (puertos y adaptadores) aporta valor cuando existen
**múltiples adaptadores intercambiables** (varias fuentes de datos, varios
protocolos de entrada) y se quiere un dominio totalmente aislado del framework. En
este MVP hay **una sola API REST y una sola base de datos**, sin integraciones
externas. Introducir puertos con una única implementación sería *over-engineering*:
más clases y ceremonia sin un eje de cambio real que lo justifique (YAGNI).

Aun así, se capturan las ventajas clave que aporta hexagonal **sin su costo**:

- El dominio y la lógica están aislados del transporte (DTOs ≠ entidades).
- La lógica vive en el `service`, no en el `controller`.
- Las dependencias apuntan hacia adentro y el tiempo se inyecta (`Clock`), lo que
  hace el núcleo **testeable de forma determinista**.

Es una decisión proporcional al problema y al plazo, priorizando la **claridad** y
la **profundidad de las reglas de negocio y las pruebas**.

---

## Estructura del proyecto

```
src/main/java/com/medisalud/agendamiento
├── config/         DataLoader (carga inicial), OpenApiConfig (Swagger), TimeConfig (Clock)
├── controller/     MedicoController, PacienteController, CitaController
├── domain/         Medico, Paciente, Cita, Penalizacion, EstadoCita
├── dto/            *Request / *Response (records con validación)
├── exception/      ErrorResponse, GlobalExceptionHandler, excepciones de dominio
├── repository/     *Repository (Spring Data JPA) + CitaSpecifications
└── service/        MedicoService, PacienteService, CitaService, HorarioAtencionService
```

---

## Cómo ejecutar el proyecto

### Requisitos

- **JDK 21** (por ejemplo Amazon Corretto 21 o Eclipse Temurin 21).
- No hace falta instalar Maven: se incluye el wrapper (`mvnw` / `mvnw.cmd`).

### Opción A — Maven wrapper

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

### Opción B — Empaquetar y ejecutar el JAR

```bash
./mvnw clean package
java -jar target/agendamiento-0.0.1-SNAPSHOT.jar
```

### Opción C — Desde el IDE

Ejecutar la clase `AgendamientoApplication` (botón ▶ en IntelliJ / VS Code).

La aplicación queda disponible en **http://localhost:8080** y carga automáticamente
los 3 médicos de ejemplo la primera vez.

---

## Documentación interactiva (Swagger)

Con la aplicación en marcha:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

Desde Swagger UI se pueden probar todos los endpoints con "Try it out".

### Consola de base de datos (H2)

Por seguridad, la consola H2 está **desactivada por defecto** (no debe exponerse en
producción). Se habilita solo con el perfil `dev`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# o con el JAR:
java -jar target/agendamiento-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

- **H2 Console** (solo perfil `dev`): http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/medisalud` · Usuario: `sa` · Contraseña: *(vacía)*

---

## Configuración

Parámetros en `src/main/resources/application.properties` (todas las reglas son
configurables sin tocar código):

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `app.zona-horaria` | `America/Bogota` | Zona horaria única de la clínica |
| `app.festivos` | `2026-01-01,2026-05-01,2026-12-25` | Días festivos sin atención (RN-01) |
| `app.penalizaciones.umbral` | `3` | Nº de penalizaciones que bloquean el agendamiento (RN-05) |
| `app.penalizaciones.ventana-dias` | `30` | Ventana en días para contar penalizaciones (RN-05) |
| `app.disponibilidad.max-dias` | `31` | Rango máximo permitido al consultar disponibilidad (RF-04) |

> **Zona horaria y fechas:** todas las fechas/hora se interpretan en la zona de la
> clínica. Se usa `LocalDateTime` en formato ISO 8601 (ej. `2026-07-06T10:00:00`).

---

## Endpoints de la API

Base URL: `http://localhost:8080`

### Médicos (RF-01)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/medicos` | Registrar un médico |
| `GET` | `/api/medicos` | Listar médicos |
| `GET` | `/api/medicos/{id}` | Obtener un médico |

**Ejemplo — crear médico**
```http
POST /api/medicos
Content-Type: application/json

{
  "nombreCompleto": "Dra. Laura Méndez",
  "especialidad": "Neurología",
  "telefono": "555-2020",
  "email": "laura.mendez@medisalud.com"
}
```
```json
201 Created
{
  "id": 4,
  "nombreCompleto": "Dra. Laura Méndez",
  "especialidad": "Neurología",
  "telefono": "555-2020",
  "email": "laura.mendez@medisalud.com"
}
```

### Pacientes (RF-02)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/pacientes` | Registrar un paciente |
| `GET` | `/api/pacientes` | Listar pacientes |
| `GET` | `/api/pacientes/{id}` | Obtener un paciente |

**Ejemplo — crear paciente**
```http
POST /api/pacientes
Content-Type: application/json

{
  "nombreCompleto": "Juan Pérez",
  "documento": "1234567",
  "telefono": "5551234",
  "email": "juan.perez@mail.com",
  "fechaNacimiento": "1990-05-20"
}
```
```json
201 Created
{
  "id": 1,
  "nombreCompleto": "Juan Pérez",
  "documento": "1234567",
  "telefono": "5551234",
  "email": "juan.perez@mail.com",
  "fechaNacimiento": "1990-05-20"
}
```

### Citas (RF-03 a RF-06)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/citas` | Reservar una cita (RF-03) |
| `GET` | `/api/citas/disponibilidad` | Franjas disponibles de un médico (RF-04) |
| `GET` | `/api/citas` | Listar citas con filtros (RF-06) |
| `GET` | `/api/citas/{id}` | Obtener una cita |
| `POST` | `/api/citas/{id}/cancelar` | Cancelar una cita (RF-05) |
| `PUT` | `/api/citas/{id}/reprogramar` | Reprogramar una cita (RN-06) |

**Reservar cita**
```http
POST /api/citas
Content-Type: application/json

{ "pacienteId": 1, "medicoId": 1, "fechaHora": "2026-07-06T10:00:00" }
```
```json
201 Created
{
  "id": 1,
  "pacienteId": 1,
  "pacienteNombre": "Juan Pérez",
  "medicoId": 1,
  "medicoNombre": "Dra. María González",
  "especialidad": "Cardiología",
  "fechaHora": "2026-07-06T10:00:00",
  "estado": "PROGRAMADA",
  "fechaCancelacion": null
}
```

**Consultar disponibilidad**
```http
GET /api/citas/disponibilidad?medicoId=1&fechaInicio=2026-07-06&fechaFin=2026-07-06
```
```json
200 OK
[
  { "inicio": "2026-07-06T08:00:00", "fin": "2026-07-06T08:30:00" },
  { "inicio": "2026-07-06T08:30:00", "fin": "2026-07-06T09:00:00" }
]
```

**Listar citas (filtros opcionales)** — `medicoId`, `pacienteId`, `estado`,
`fechaInicio`, `fechaFin`:
```http
GET /api/citas?medicoId=1&estado=PROGRAMADA&fechaInicio=2026-07-01T00:00:00&fechaFin=2026-07-31T23:59:59
```

**Cancelar cita**
```http
POST /api/citas/1/cancelar
```
```json
200 OK
{ "id": 1, "estado": "CANCELADA", "fechaCancelacion": "2026-07-06T09:30:00", ... }
```

**Reprogramar cita**
```http
PUT /api/citas/1/reprogramar
Content-Type: application/json

{ "nuevaFechaHora": "2026-07-07T11:00:00" }
```

---

## Reglas de negocio

| Regla | Descripción | Implementación |
|---|---|---|
| **RN-01** | Horario: L-V 08:00–18:00, Sáb 08:00–13:00, sin domingos ni festivos; franjas de 30 min | `HorarioAtencionService` genera y valida las franjas |
| **RN-02** | Un médico no puede tener dos citas en la misma franja | Verificación previa a la reserva → `409` |
| **RN-03** | No se agenda a pacientes con fecha de nacimiento futura (sin fecha ⇒ edad 0, permitido) | `CitaService.validarEdad` |
| **RN-04** | Un paciente no puede tener dos citas en la misma franja | Verificación previa a la reserva → `409` |
| **RN-05** | Cancelar con < 2 h de antelación genera penalización; con ≥ 3 penalizaciones en 30 días se bloquea el agendamiento | `Penalizacion` + conteo por ventana temporal |
| **RN-06** | Reprogramar = cancelar la anterior (aplicando RN-05) + crear una nueva validando disponibilidad | `CitaService.reprogramar` (transaccional) |

### Nota sobre RN-04 (interpretación)

El enunciado de RN-04 mezcla "con el mismo médico" con "aunque sea otro médico". La
restricción de "mismo médico en la misma franja" ya queda cubierta por RN-02 (un
médico solo puede tener una cita por franja). Por eso RN-04 se implementó con la
lectura que aporta valor real: **un paciente no puede tener dos citas en la misma
franja horaria, aunque sean con médicos distintos** (no puede estar en dos lugares a
la vez). Esta decisión está documentada aquí y encapsulada en
`CitaService.validarPacienteSinConflicto`.

### Determinismo temporal

Las reglas dependientes del tiempo (RN-05) usan un `Clock` inyectado
(`TimeConfig`) en lugar de `LocalDateTime.now()` directo. Esto permite fijar el
"ahora" en las pruebas y validar de forma reproducible los bordes de "< 2 horas" y
la ventana de 30 días.

---

## Manejo de errores

Todas las respuestas de error comparten un cuerpo uniforme, producido por
`GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-07-06T09:30:00.123",
  "status": 409,
  "error": "Conflict",
  "message": "El medico ya tiene una cita programada en esa franja horaria",
  "path": "/api/citas",
  "fieldErrors": null
}
```

| Código | Cuándo |
|---|---|
| `400 Bad Request` | Validación de entrada fallida (`fieldErrors` detalla cada campo), parámetro mal tipado o JSON malformado |
| `404 Not Found` | Recurso inexistente (médico, paciente o cita) |
| `409 Conflict` | Violación de regla de negocio (franja ocupada, horario inválido, penalizaciones, documento duplicado, etc.) |
| `500 Internal Server Error` | Error no controlado (sin exponer detalles internos; se registran en el log) |

---

## Pruebas

```bash
./mvnw test
```

La suite cubre los flujos críticos y casos borde:

- **`HorarioAtencionServiceTest`** — generación de franjas (20 entre semana, 10 los
  sábados, 0 domingos/festivos) y validación de franjas (alineación a 30 min, límites).
- **`CitaServiceTest`** — reserva feliz y rechazo por cada regla (RN-01 a RN-05),
  cancelación con y sin penalización, cancelación de cita no programada, y cálculo
  de disponibilidad excluyendo franjas ocupadas. Usa un `Clock` fijo.
- **`PacienteServiceTest`** — alta, documento duplicado y recurso inexistente.
- **`MedicoControllerTest` / `CitaControllerTest`** — capa web (MockMvc): códigos
  HTTP 201/400/409/200 y forma de la respuesta.

---

## Despliegue

### Docker (local)

El proyecto incluye un `Dockerfile` multi-stage (build + runtime con JRE 21):

```bash
docker build -t medisalud-agendamiento .
docker run -p 8080:8080 medisalud-agendamiento
```

### Nube

La imagen es desplegable en cualquier PaaS que soporte contenedores
(Render, Railway, Fly.io, Google Cloud Run, etc.). Pasos típicos:

1. Publicar el repositorio en GitHub.
2. Crear un servicio web desde el repo apuntando al `Dockerfile`.
3. Exponer el puerto `8080`.

> Para producción se recomienda migrar de H2 en archivo a una base de datos
> gestionada (PostgreSQL) ajustando el `application.properties`.

---

## Decisiones y limitaciones conocidas

- **H2 en archivo** (`./data/medisalud`): elegida para un MVP autocontenido que no
  requiere instalar un motor externo, conservando los datos entre reinicios. La
  capa JPA permite cambiar a PostgreSQL/MySQL sin tocar la lógica.
- **Concurrencia**: la no-duplicidad (RN-02/RN-04) se valida dentro de la
  transacción de reserva. Para un escenario de alta concurrencia con múltiples
  instancias, se reforzaría con un índice único parcial o bloqueo optimista; se
  documenta como mejora futura por estar fuera del alcance del MVP.
- **Sin autenticación/autorización**: fuera de alcance de la prueba.
- **Estado `ATENDIDA`**: contemplado en el modelo (`EstadoCita`) y en los filtros de
  listado; la transición se implementaría en una iteración posterior.
