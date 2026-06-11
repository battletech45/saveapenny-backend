# Testing Guide

## Overview

SaveAPenny has 830+ tests covering unit, integration, and regression scenarios. The test suite is the primary verification gate.

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

## Test Categories

### Unit Tests

Pure JUnit 5 + Mockito tests. No Spring context loaded. Fastest tier.

```
src/test/java/.../service/impl/*Test.java
src/test/java/.../mapper/*Test.java
```

Run in milliseconds. Cover service logic, mappers, and validation.

### Slice Tests (`@WebMvcTest`)

Controller tests loading only the web layer with mocked services. Test HTTP request/response mapping, validation, and error handling.

```
src/test/java/.../controller/*Test.java
```

Run in ~100ms each. Import `SecurityConfig` and `HeaderUserAuthenticationFilter` for auth context.

### Repository Tests (`@DataJpaTest`)

H2-backed repository tests. Cover queries, sorting, pagination, and constraints.

```
src/test/java/.../repository/*Test.java
```

Run in ~30ms each.

### Integration Tests (`@SpringBootTest`)

Full Spring context with H2 in PostgreSQL mode and `ddl-auto=create-drop`. Test end-to-end flows across multiple layers.

```
src/test/java/.../integration/*IntegrationTest.java
```

Each test class uses `@TestPropertySource` with an isolated H2 database (`jdbc:h2:mem:<unique-name>`) so they do not interfere with each other.

### Golden Image Regression

The OCR golden image test (`OcrGoldenImageRegressionTest.java`) runs with H2 and skips via `Assumptions.assumeTrue` when OCR is disabled or Tesseract is not installed. It is not part of the default CI path.

## Test Conventions

| Convention | Standard |
|------------|----------|
| Naming | `{method}_when{Condition}_expects{Result}` |
| Assertions | AssertJ or JUnit 5 assertions |
| Mocking | Mockito with `@ExtendWith(MockitoExtension.class)` |
| Test data | Constructed manually (no test fixtures framework) |

## Integration Test Template

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
    void myTest() throws Exception {
        mockMvc.perform(post("/api/v1/..."))
                .andExpect(status().isOk());
    }
}
```

## Preventing Flaky Tests

- Integration tests use unique in-memory databases per class
- H2 is configured in PostgreSQL compatibility mode (`MODE=PostgreSQL`)
- Repository tests reset state between methods via `@DataJpaTest` rollback
- No shared mutable state between test classes
- OCR tests use `Assumptions.assumeTrue` to skip when native dependencies are unavailable
