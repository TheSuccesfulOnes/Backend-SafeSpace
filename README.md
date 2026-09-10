# Backend API

Backend modular para la aplicación de bienestar laboral. El proyecto utiliza Java 21, Spring Boot, MySQL, Flyway, JWT y OpenAPI/Swagger.

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL local con la base `employee_wellbeing_db`
- Usuario de aplicación `employee_app`

## Ejecución local

```bash
mvn spring-boot:run
```

Swagger estará disponible en `http://localhost:8080/swagger-ui.html`.

La configuración local usa variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`. El administrador local se inicializa como `admin` / `admin123` únicamente cuando está activo el perfil `local`.

### Gemini para el chat de empleados

El chat de IA solo está disponible para cuentas `EMPLOYEE`. Para activarlo localmente, define `GEMINI_API_KEY` como variable de entorno antes de ejecutar Spring Boot; nunca la guardes en el repositorio, en `application.yml` ni en la aplicación móvil:

```powershell
$env:GEMINI_API_KEY = "TU_CLAVE_NUEVA"
mvn spring-boot:run
```

La integración usa `gemini-3.5-flash-lite` por defecto y puede cambiarse con `GEMINI_MODEL`. También admite `AI_ENABLED`, `AI_MAX_INPUT_CHARACTERS`, `AI_MAX_OUTPUT_TOKENS`, `AI_MAX_OUTPUT_CHARACTERS` y `AI_MAX_HISTORY_MESSAGES`. Si no se configura una clave, el backend conserva una respuesta local segura para que la aplicación pueda probarse sin un proveedor externo.

La clave se envía únicamente desde el backend mediante el encabezado `x-goog-api-key`. Las respuestas del proveedor no se registran y los errores externos se convierten en un mensaje genérico para el cliente.

## Endpoints iniciales

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/password-recovery/request` (solicitud genérica, solo empleados)
- `POST /api/v1/auth/password-recovery/confirm` (confirma un token de un solo uso)
- `GET /api/v1/admin/users` (solo `SYSTEM_ADMIN`)
- `POST /api/v1/admin/hr-members` (solo `SYSTEM_ADMIN`)

Human Resources accounts remain local accounts created by `SYSTEM_ADMIN`; they do not require an email address in the HR creation flow.

### Recuperación de contraseña

La recuperación está disponible únicamente para empleados habilitados con correo. La respuesta de solicitud es siempre genérica para evitar enumerar cuentas. Los tokens se almacenan como SHA-256, expiran en 15 minutos, solo pueden utilizarse una vez y las solicitudes se limitan por identificador.

En el perfil `local`, el adaptador de notificación escribe el enlace temporal en el log del backend para poder probar el flujo sin contratar un servicio de correo. En otros perfiles no se expone el token; se debe conectar un adaptador de correo que implemente `PasswordResetNotificationPort`.
