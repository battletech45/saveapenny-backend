# Testing Guide

## Overview

SaveAPenny has 830+ tests covering unit, integration, and regression scenarios. Tests are the primary verification gate and run on every PR and push to main via GitHub Actions.

## Prerequisites

- **Java 24** — required for running tests
- **Docker** — required for Testcontainers-based integration tests
- **Tesseract** — required only for OCR golden-image regression tests

## Running Tests

```bash
# Full suite
mvn test

# Single test class
mvn -Dtest=AuthFlowIntegrationTest test

# Single test method
mvn -Dtest=AuthFlowIntegrationTest#loginThenRefresh_thenOldTokenFails test

# Package without tests
mvn package -DskipTests
```

## Test Types

| Type | Spring Context | Docker | Typical Speed | Location |
|------|---------------|--------|---------------|----------|
| Unit | No | No | Milliseconds | `.../service/impl/*Test.java`, `.../mapper/*Test.java` |
| Slice (`@WebMvcTest`) | Web layer only | No | ~100ms | `.../controller/*Test.java` |
| Repository (`@DataJpaTest`) | JPA only | No | ~30ms | `.../repository/*Test.java` |
| Integration (`@SpringBootTest`) | Full | Mostly H2; a few use Testcontainers (`TestcontainersIntegrationTest`, `TransactionFlowIntegrationTest`, `FullFlowIntegrationTest`, `ImportFlowIntegrationTest`, `ReportFlowIntegrationTest`, `GoalSimulationFlowIntegrationTest`) | Seconds | `.../integration/*IntegrationTest.java` |
| Golden Image | Web + JPA | No (H2) | Seconds | `.../ocr/.../OcrGoldenImageRegressionTest.java` |

### Unit Tests

Pure JUnit 5 + Mockito tests. No Spring context loaded. Cover service logic, mappers, and validation.

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyServiceImpl service;

    @Test
    void calculateTotal_whenMultipleItems_returnsSum() {
        // pure logic, no Spring
    }
}
```

### Slice Tests (`@WebMvcTest`)

Controller tests loading only the web layer with mocked services. Test HTTP request/response mapping, validation, and error handling.

```java
@WebMvcTest(MyController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class MyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyService myService;

    @Test
    void getEndpoint_whenAuthenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/...")
                        .header("Authorization", "Bearer " + validJwt))
                .andExpect(status().isOk());
    }
}
```

### Repository Tests (`@DataJpaTest`)

H2-backed repository tests covering queries, sorting, pagination, and constraints.

### Integration Tests (`@SpringBootTest`)

Full Spring context with H2 in PostgreSQL compatibility mode. Each test class uses an isolated in-memory database via `@TestPropertySource`:

```java
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:my-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class MyIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void endToEndFlow_whenValidRequest_succeeds() throws Exception {
        mockMvc.perform(post("/api/v1/..."))
                .andExpect(status().isOk());
    }
}
```

### Golden Image Regression (OCR)

The OCR golden image test (`OcrGoldenImageRegressionTest.java`) runs with H2 and skips via `Assumptions.assumeTrue` when OCR is disabled or Tesseract is not installed.

## Test Conventions

| Convention | Standard |
|------------|----------|
| Naming | `{method}_when{Condition}_expects{Result}` |
| Assertions | AssertJ or JUnit 5 |
| Mocking | Mockito with `@ExtendWith(MockitoExtension.class)` |
| Test data | Constructed manually (no test fixtures framework) |

## Preventing Flaky Tests

| Practice | Detail |
|----------|--------|
| Unique databases | Integration tests use isolated in-memory H2 databases per class |
| PostgreSQL compatibility | H2 configured with `MODE=PostgreSQL` for dialect compatibility |
| Automatic rollback | `@DataJpaTest` rolls back between methods |
| No shared state | No mutable state shared between test classes |
| Conditional skipping | OCR tests use `Assumptions.assumeTrue` when native deps are missing |

## CI Pipeline

Tests run across three GitHub Actions workflows:

| Workflow | Command | When |
|----------|---------|------|
| validate.yml | `mvn -B -DskipTests package` | Every PR and push to main |
| ci.yml | `mvn -B clean verify` | Every PR and push to main |
| test.yml | `mvn -B clean verify` | Every PR and push to main (dedicated, with OCR verification) |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| H2 over Testcontainers for most tests | Faster (in-memory, no container startup), still PostgreSQL-compatible |
| Unique H2 databases per test class | Parallel-safe, no cross-class interference |
| `ddl-auto=create-drop` in tests | No Flyway setup needed per test; schema derived from entities |
| `@TestPropertySource` over `application-test.yml` | Explicit isolation; each test owns its datasource URL |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/test/java/.../config/security/SecurityConfigTest.java` | Security filter chain tests |
| `src/test/java/.../config/security/RateLimiterTest.java` | Token bucket unit tests |
| `src/test/java/.../config/security/RateLimitingFilterTest.java` | Rate limiting filter tests |
| `src/test/java/.../config/security/HeaderUserAuthenticationFilterTest.java` | JWT filter tests |
| `src/test/java/.../integration/` | End-to-end integration test classes |
