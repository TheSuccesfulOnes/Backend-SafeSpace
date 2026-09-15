# SafeSpace Backend API

Backend modular de SafeSpace para bienestar laboral. Está construido con Java 21 y Spring Boot, usa Firestore como única persistencia, JWT para autenticación y OpenAPI/Swagger para documentar la API.

## Requisitos

- JDK 21
- Maven 3.9+
- Un proyecto de Firebase con Firestore habilitado
- Credenciales de Firebase mediante Application Default Credentials

## Configuración local

No se necesita MySQL. Configura las credenciales fuera del repositorio:

```powershell
$env:FIREBASE_PROJECT_ID = "safespace-dev-f82ac"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ruta\segura\firebase-service-account.json"
$env:JWT_SECRET = "una-clave-local-de-al-menos-32-caracteres"
mvn spring-boot:run
```

La API estará disponible en `http://localhost:8080` y Swagger en `http://localhost:8080/swagger-ui.html`.

El perfil `local` crea un administrador inicial solamente si no existe:

- Usuario: `admin`
- Contraseña: `admin123`

Para cambiarlo, usa `LOCAL_ADMIN_USERNAME` y `LOCAL_ADMIN_PASSWORD`. No uses estas credenciales en producción.

## Firestore

Los adaptadores de `shared/infrastructure/firebase` almacenan los agregados en colecciones de Firestore y generan IDs numéricos mediante transacciones. Las relaciones se guardan como snapshots escalares dentro del documento para evitar joins y lazy loading.

Colecciones principales:

- `users`, `user_preferences`
- `surveys`, `survey_answers`
- `weekly_activities`, `activity_votes`
- `comments`, `comment_likes`
- `reports`, `payments`
- `ai_conversations`, `ai_messages`
- `password_reset_tokens`, `audit_logs`

Las migraciones SQL históricas no forman parte del arranque: el proyecto no incluye Flyway, JPA ni el driver de MySQL en tiempo de ejecución.

## Render y GitHub

El repositorio correcto es `https://github.com/TheSuccesfulOnes/Backend-SafeSpace.git`. El `Dockerfile`, `render.yaml` y el workflow de CI están en la raíz y usan la rama `master`.

En Render crea un servicio web desde ese repositorio y carga el archivo de cuenta de servicio como Secret File con el nombre exacto `firebase-service-account.json`. Render lo expondrá en `/etc/secrets/firebase-service-account.json`, que es la ruta configurada por `GOOGLE_APPLICATION_CREDENTIALS`.

Completa también en Render los valores marcados como secretos en `render.yaml`:

- `CORS_ALLOWED_ORIGINS`: orígenes exactos de los frontends, separados por comas; no uses `*`.
- `PASSWORD_RESET_URL_BASE`: URL pública del flujo de recuperación.
- `GEMINI_API_KEY`: opcional para el chat AI de empleados.
- `JWT_SECRET`: se genera automáticamente en el primer despliegue.

Nunca subas el JSON de Firebase, claves de Gemini, claves JWT ni otros secretos a GitHub. Si un secreto se expone, revócalo y genera uno nuevo.

El health check de Render usa `/v3/api-docs`. Una vez desplegado, Swagger estará en `https://TU-SERVICIO.onrender.com/swagger-ui.html` y la especificación OpenAPI en `https://TU-SERVICIO.onrender.com/v3/api-docs`.

## Verificación

```powershell
mvn test
mvn -DskipTests package
```

El CI ejecuta las pruebas automáticamente en cada push o pull request hacia `master`.
