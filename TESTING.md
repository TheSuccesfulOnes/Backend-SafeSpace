# Backend validation tests

## Run locally

Java 21 or newer and Maven are required. The repository uses the existing JUnit, AssertJ, Mockito, Spring Test, Hibernate Validator and Jackson dependencies; no dependency was added and `pom.xml` is unchanged.

From this repository:

```powershell
pwsh -NoProfile -File tests/run-tests.ps1
```

The wrapper runs Maven tests, Spotless verification, package compilation and the inventory verifier, in that order. It restores the caller's location and JAVA_HOME. Override the default JDK with `-JavaHome 'C:\Program Files\Android\Android Studio\jbr'`.

To format only changed/untracked Java files and regenerate the reviewed inventory:

```powershell
pwsh -NoProfile -File tests/run-tests.ps1 -Format -RefreshInventory
```

The underlying commands are `mvn test`, `mvn spotless:check`, and `mvn -DskipTests package`. For a fresh audit without stale Surefire XML, first run `mvn clean` with the same JDK. Targeted test runs are useful for diagnosis but do not replace the full-run inventory check.

## Verified execution

The independently verified full Maven run produced **965 executed cases in 59 suites, with zero failures, errors or skipped cases**. Spotless verification and executable-JAR packaging also passed. The existing cases were retained; login regression coverage adds four unit invocations and three local HTTP integration cases.

All **45 inventoried validation owners have at least 20 executed cases**. Their mapped suites total **962 cases: 491 unit and 471 local integration**; two supplementary suites contribute another three cases (1 unit and 2 local integration). These are executed test invocations, never assertion counts or code-coverage percentages.

FirebaseConfigValidationTest passed all 20 scenarios (12 unit and 8 local Spring/SDK integration). Its assertions use public SDK contracts; the credential fixture permits local scoping while explicitly forbidding token refresh and request-metadata retrieval. All ADC/app/Firestore entry points remain intercepted.

`tests/verify-inventory.ps1` successfully regenerated `src/test/resources/validation-inventory.json` from the complete Surefire XML and verified every owner quota. The source-to-suite input manifest is `tests/validation-sources.json`. The initial ordered-dictionary counting issue was corrected by using PSCustomObject rows before aggregation; no counts are manually fabricated.

## What unit and integration mean

- **U / unit:** a focused validator, entity, service or security component with collaborators mocked; DTO units use actual Hibernate Validator and Jackson.
- **I / local integration:** actual component boundaries composed locally: controller + Jackson + validation + service/repository mocks; Spring Security + JWT filter + guarded controller; Spring Binder/configuration contexts; repository/domain behavior using in-memory reads or mocked Firestore SDK; or RestClient request/response contracts intercepted by MockRestServiceServer.
- These are **not live backend, emulator, provider or end-to-end tests**. Standalone DTO/service HTTP tests do not enable authentication; dedicated SecurityConfig and guarded-controller suites test real route/method authorization separately.
- Dynamic and parameterized invocations count individually in Surefire. A factory method is not counted as one case. Existing service tests are counted alongside new suites, without deletion or double counting.
- The generated JSON records every actual XML testcase name, result and unit/integration type under source -> suites -> cases. Its top-level totals must match the full run; the verifier rejects missing/unmapped suites, owners under 20, skipped/failed cases and inventory drift. No coverage instrumenter was introduced.

## Complete validation-owner inventory

Source paths below are relative to `src/main/java/com/experimentos/backend/`. Suites are under `src/test/java/com/experimentos/backend/validation/` unless the source's native package applies: configuration/security native suites, authentication application/domain suites, AiPropertiesValidationTest in ai/application, and LocalAdminInitializerValidationTest in iam/application. The authoritative manifest stores exact fully qualified suite names. Each suite entry below is **executed total (unit/local integration)** from the current XML, not planned assertions.

| Source | Suite(s): executed cases (U/I) |
| --- | --- |
| `activity/interfaces/ActivityDtos.java` | `ActivityDtosValidationTest`: 20 (10U/10I) |
| `activity/interfaces/ActivityAdminDtos.java` | `ActivityAdminDtosValidationTest`: 20 (10U/10I) |
| `authentication/interfaces/AuthDtos.java` | `AuthDtosValidationTest`: 20 (10U/10I) |
| `admin/interfaces/AdminDtos.java` | `AdminDtosValidationTest`: 20 (10U/10I) |
| `ai/interfaces/AiDtos.java` | `AiDtosValidationTest`: 20 (10U/10I) |
| `comment/interfaces/CommentDtos.java` | `CommentDtosValidationTest`: 20 (10U/10I) |
| `mood/interfaces/MoodDtos.java` | `MoodDtosValidationTest`: 20 (6U/14I) |
| `profile/interfaces/ProfileDtos.java` | `ProfileDtosValidationTest`: 20 (10U/10I) |
| `report/interfaces/ReportDtos.java` | `ReportDtosValidationTest`: 20 (10U/10I) |
| `survey/interfaces/SurveyDtos.java` | `SurveyDtosValidationTest`: 20 (10U/10I) |
| `survey/interfaces/SurveyAdminDtos.java` | `SurveyAdminDtosValidationTest`: 20 (10U/10I) |
| `authentication/application/AuthService.java` | `AuthServiceValidationTest`: 23 (12U/11I)<br>`AuthServiceTest`: 15 (15U/0I) |
| `admin/application/AdminService.java` | `AdminServiceValidationTest`: 20 (12U/8I)<br>`AdminServiceTest`: 9 (9U/0I) |
| `profile/application/ProfileService.java` | `ProfileServiceValidationTest`: 24 (14U/10I) |
| `payment/application/PaymentService.java` | `PaymentServiceValidationTest`: 20 (14U/6I)<br>`PaymentServiceTest`: 4 (4U/0I) |
| `survey/application/SurveyService.java` | `SurveyServiceValidationTest`: 20 (12U/8I)<br>`SurveyServiceTest`: 3 (3U/0I) |
| `survey/application/AdminSurveyService.java` | `AdminSurveyServiceValidationTest`: 20 (12U/8I)<br>`AdminSurveyServiceTest`: 3 (3U/0I) |
| `comment/application/CommentService.java` | `CommentServiceValidationTest`: 20 (12U/8I)<br>`CommentServiceTest`: 3 (3U/0I) |
| `activity/application/ActivityService.java` | `ActivityServiceValidationTest`: 20 (12U/8I) |
| `activity/application/AdminActivityService.java` | `AdminActivityServiceValidationTest`: 20 (12U/8I)<br>`AdminActivityServiceTest`: 3 (3U/0I) |
| `ai/application/AiChatService.java` | `AiChatServiceValidationTest`: 21 (13U/8I)<br>`AiChatServiceTest`: 3 (3U/0I) |
| `mood/application/MoodService.java` | `MoodServiceValidationTest`: 20 (12U/8I)<br>`MoodServiceTest`: 4 (4U/0I) |
| `report/application/ReportService.java` | `ReportServiceValidationTest`: 20 (12U/8I)<br>`ReportServiceTest`: 1 (1U/0I) |
| `authentication/application/PasswordResetService.java` | `PasswordResetServiceValidationTest`: 20 (12U/8I)<br>`PasswordResetServiceTest`: 3 (3U/0I) |
| `authentication/application/PasswordResetRateLimiter.java` | `PasswordResetRateLimiterValidationTest`: 20 (12U/8I) |
| `authentication/domain/RegistrationPasswordPolicy.java` | `RegistrationPasswordPolicyValidationTest`: 20 (20U/0I)<br>`RegistrationPasswordPolicyTest`: 3 (3U/0I) |
| `authentication/domain/PasswordResetToken.java` | `PasswordResetTokenValidationTest`: 20 (12U/8I) |
| `ai/application/AiProperties.java` | `AiPropertiesValidationTest`: 20 (12U/8I) |
| `authentication/application/PasswordResetProperties.java` | `PasswordResetPropertiesValidationTest`: 20 (12U/8I) |
| `shared/security/CorsConfig.java` | `CorsConfigValidationTest`: 20 (12U/8I) |
| `shared/security/JwtService.java` | `JwtServiceValidationTest`: 20 (8U/12I) |
| `shared/security/JwtAuthenticationFilter.java` | `JwtAuthenticationFilterValidationTest`: 20 (12U/8I) |
| `shared/security/CurrentUser.java` | `CurrentUserValidationTest`: 20 (12U/8I) |
| `shared/security/SecurityConfig.java` | `SecurityConfigValidationTest`: 20 (4U/16I) |
| `shared/infrastructure/firebase/configuration/FirebaseConfig.java` | `FirebaseConfigValidationTest`: 20 (12U/8I) |
| `iam/application/LocalAdminInitializer.java` | `LocalAdminInitializerValidationTest`: 20 (12U/8I) |
| `ai/infrastructure/GeminiAiAdapter.java` | `GeminiAiAdapterValidationTest`: 20 (2U/18I) |
| `shared/infrastructure/firebase/repositories/AbstractFirestoreRepository.java` | `FirestoreValidationTest`: 20 (12U/8I) |
| `comment/domain/Comment.java` | `CommentOwnershipValidationTest`: 20 (8U/12I) |
| `report/domain/Report.java` | `ReportPrivacyValidationTest`: 20 (8U/12I) |
| `activity/interfaces/ActivityController.java` | `ActivityControllerSecurityTest`: 20 (0U/20I) |
| `survey/interfaces/SurveyController.java` | `SurveyControllerSecurityTest`: 20 (0U/20I) |
| `mood/interfaces/MoodController.java` | `MoodControllerSecurityTest`: 20 (0U/20I) |
| `report/interfaces/ReportController.java` | `ReportControllerSecurityTest`: 20 (0U/20I) |
| `ai/interfaces/AiChatController.java` | `AiChatControllerSecurityTest`: 20 (0U/20I) |

Supplementary retained suites, not separate validation owners:

| Suite | Executed cases | Type |
| --- | ---: | --- |
| shared/configuration/OpenApiConfigTest | 1 | Unit: documentation configuration |
| shared/infrastructure/firebase/repositories/CompositeIdRepositoryTest | 2 | Local integration: entity/composite-id mapping through mocked SDK |

RegistrationPasswordPolicy's controller/service integration is additionally exercised by AuthServiceValidationTest. Those invocations remain counted once under AuthService, rather than duplicating global counts under the policy source.

Login automation uses the existing `POST /api/v1/auth/login` JSON contract (`identifier`, `password`). AuthService trims the username/email before account lookup and preserves the password exactly. HTTP regression cases cover padded usernames, padded emails with significant password spaces, and blank identifiers rejected before persistence access. HTML IDs and native resource tags belong to the clients and are not additional API fields.

## Meaningful scenario coverage

DTO suites exercise valid payload decoding and required/null/blank/length boundaries, nested request constraints, enum/JSON shape errors and rejected HTTP requests that must not call services. HTTP helpers use the production SNAKE_CASE Jackson strategy, fresh mocks/security contexts per dynamic case, exactly one invocation on valid submissions and actual decoded argument checks.

Service suites cover registration/login permissions and uniqueness; password recovery's opaque response, hashed token, expiry, replay, invalidation and quotas; profile language/theme handling; PDF multipart signature/type/size/name guards and month/year renewal boundaries; survey/activity lifecycle and immutability; comment ownership/likes/deletion; mood role/date/duplicate/rate rules; AI conversation ownership/history/provider failure compensation; and report anonymity/status/privacy.

Security suites include malformed/expired/tampered/unsigned/wrong-key tokens, current database roles overriding signed claims, missing/disabled accounts, unauthenticated/anonymous identities, route restrictions and real @PreAuthorize enforcement. Each guarded-controller matrix exercises its representative protected read flow for all roles plus privilege changes and invalid tokens; this does not imply every controller method has an independent 20-case matrix.

Persistence/configuration suites cover encoding/hydration/identity failures and interrupted SDK calls, local-admin ownership/idempotence/profile activation, CORS HTTP preflights, bounded property binding and intercepted Gemini candidate/response/error contracts. The Firebase suite intercepts all static ADC/app/Firestore entry points and performs no real initialization or credential loading.

## Isolation and deterministic fixtures

No @SpringBootTest boots the real application. Repositories, SDK endpoints and notification/provider collaborators are mocked or in memory. Gemini requests use MockRestServiceServer with a synthetic .invalid base URL; unexpected provider calls fail the test. Firebase tests statically intercept GoogleCredentials.getApplicationDefault, FirebaseApp discovery/initialization and FirestoreClient construction, so no credential file is opened and no real Firebase app is initialized.

Day rollover, rate-limit expiry and payment renewals use fixed/mutable clocks; no long sleeps are used. Security contexts are cleared around dynamic scenarios, shared mocks are reset between security/DTO cases, scoped static mocks and Spring contexts close, and concurrency executors terminate in finally blocks. There is no persistent browser storage or real provider state to clear. Synthetic token secrets and email addresses are test data only.

## Excluded scan candidates and reasons

Exclusion means “not an independent validator”, not “never exercised”. Delegated rules are counted at their actual owner; response mappings, plain assignments and factory/equality checks are not expanded into artificial 20-case suites.

| Candidate (backend-relative unless glob shown) | Reason |
| --- | --- |
| `iam/domain/UserPreferences.java` | Stores values without validating them; supported languages/themes are enforced by ProfileService. Getters/defaults are not additional validation owners. |
| `iam/domain/User.java` | Plain entity mutations/getters; account uniqueness, roles and owner/access rules belong to services/security. |
| `activity/domain/WeeklyActivity.java` | State assignment and preserving option IDs when labels are equal; no independent rejection policy. Lifecycle/immutability rules exercised by both activity services. |
| `survey/domain/Survey.java` | State assignment and field updates; published/duplicate-answer permission rules belong to services. |
| `payment/interfaces/PaymentDtos.java` | Response mapping only; multipart input guards live in PaymentService/PaymentController binding. |
| `shared/configuration/OpenApiConfig.java` | API documentation metadata, no input/business/security enforcement. Existing documentation tests retained. |
| `ai/infrastructure/GeminiClientConfiguration.java` | Constructs a bounded HTTP client, no independent input validator. Adapter HTTP tests intercept every request. |
| `shared/interfaces/ApiExceptionHandler.java` | Maps already-raised exceptions to HTTP status/safe messages; exercised by local controller/service integrations, not an independent validator. |
| `audit/application/AuditService.java` | Records audit events; no independent input/business guard. |
| `authentication/infrastructure/PasswordResetConfiguration.java` | Enables binding for PasswordResetProperties; no independent input validation. |
| `authentication/infrastructure/LoggingPasswordResetNotificationAdapter.java` | Notification transport/logging implementation, not an input validator; no real notification delivery tested. |
| `authentication/infrastructure/UnconfiguredPasswordResetNotificationAdapter.java` | Unconfigured notification logging transport, not a data/business validation policy. |
| `authentication/domain/PasswordResetNotificationPort.java` | Interface contract only. |
| `activity/domain/ActivityVote.java` | Composite-key equals/hashCode and stored entity fields; identity mapping regressions retained without treating factory/equality checks as validators. |
| `comment/infrastructure/CommentLikeEntity.java` | Composite-key equals/hashCode, not an access-control rule. |
| `authentication/infrastructure/PasswordResetTokenRepository.java` | Query/deletion delegation; token hash/usability policy is owned by PasswordResetToken and tested with this repository in memory. |
| `src/main/java/**/infrastructure/*Repository.java (except AbstractFirestoreRepository)` | Collection query filters and object mapping, not independent validation policies. Persistence error/encoding guards are counted under AbstractFirestoreRepository. |
| `src/main/java/**/interfaces/*Controller.java (without @PreAuthorize)` | Delegates DTO @Valid and routes/services; constraints are counted under the DTO source and business rules under the service, not counted again as wrapper validators. |
| `src/main/java/**/domain/{enums,response records,plain entities}.java (except Comment,Report,PasswordResetToken,RegistrationPasswordPolicy)` | Declared value sets, DTO serialization, getters and state holders without a separate validator; actual enum input rejection exercised through DTO/HTTP suites. |
| `BackendApiApplication.java` | Bootstrap entry point, no validation policy. |

## Minimal production seams and regressions

- MoodService and PaymentService retain their original @Autowired constructors; overloads accept Clock to make business-date boundaries deterministic, preserving production time-zone behavior.
- ProfileService accepts only normalized es/en languages and LIGHT/DARK themes before persistence; unsupported language regression prevents blindly storing values unsupported by clients.
- Comment.isOwnedBy fails closed for a null caller or stored author identifier.
- CurrentUser rejects AnonymousAuthenticationToken instead of treating its principal as an authenticated identity.

No production credentials or data were read or modified. Production regression corrections are committed separately from the testing infrastructure and deterministic-clock seams.

## Limitations and dependency/security audit

No dependency changes were made, including production dependencies. No new dependency needs an incremental audit; a complete vulnerability audit of the existing production dependency tree was **not performed**, and no claim is made that those dependencies are vulnerability-free.

Mocked persistence does not prove live Firestore indexes, transactions, IAM, network failure semantics, distributed races or deployed JWT/CORS configuration. Mocked notification delivery and intercepted Gemini responses do not verify a real provider. Jackson's existing enum/numeric coercion behavior was not broadly hardened; suites assert supported/rejected values actually observed, rather than assuming all coercions are forbidden. Existing retained tests may use system time; the new date-rollover cases use injected clocks.

There is no JaCoCo or other code-coverage instrumentation, so no line/branch coverage percentage is claimed. New compiler warnings were resolved using JsonNode.properties() and a type-inferred mock for ApiFuture instead of an unchecked generic class cast. The final test compilation emitted neither deprecation nor unchecked-use warnings. The initial Firebase compile error, overly broad credential-interaction assertion and inventory aggregation issue were corrected and verified by the complete passing run.
