# Technology Stack

## Overview

This document maps the technologies used in SaveAPenny to their purpose in this codebase and links to the official documentation for each one.

## Core Application Stack

| Technology | Used For In This Project | Evidence In Repo | Official Docs |
|------------|--------------------------|------------------|---------------|
| Java 24 | Main programming language and runtime for the backend service | `pom.xml`, `Dockerfile`, `docs/getting-started.md` | https://docs.oracle.com/en/java/javase/24/ |
| Maven | Build, dependency management, packaging, and test execution | `pom.xml`, `Dockerfile` | https://maven.apache.org/guides/ |
| Spring Boot 4.1 | Application bootstrap, auto-configuration, runtime wiring, and backend foundation | `pom.xml`, `src/main/java/com/saveapenny/SaveAPennyApplication.java` | https://docs.spring.io/spring-boot/documentation.html |
| Spring Web MVC | REST API controllers and HTTP request handling | `pom.xml`, `src/main/java/com/saveapenny/**/controller/*.java` | https://docs.spring.io/spring-framework/reference/web/webmvc.html |
| Spring Validation | Request DTO validation with bean validation annotations | `pom.xml`, controllers and DTOs across `src/main/java/com/saveapenny/**/dto/` | https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html |
| Spring Data JPA | Repository layer and persistence abstraction | `pom.xml`, `src/main/java/com/saveapenny/**/repository/*.java` | https://docs.spring.io/spring-data/jpa/reference/ |
| Hibernate | JPA implementation, ORM mapping, and schema validation | `src/main/resources/application.yml`, entity classes under `src/main/java/com/saveapenny/**/entity/` | https://hibernate.org/orm/documentation/ |
| PostgreSQL | Primary relational database for application data | `pom.xml`, `docker-compose.yml`, `src/main/resources/application.yml` | https://www.postgresql.org/docs/ |
| Flyway | Versioned database schema migrations | `pom.xml`, `src/main/resources/db/migration/`, `src/main/resources/application.yml` | https://documentation.red-gate.com/flyway |

## Security And API Docs

| Technology | Used For In This Project | Evidence In Repo | Official Docs |
|------------|--------------------------|------------------|---------------|
| Spring Security | Stateless API security, protected routes, custom auth filters, and method security | `pom.xml`, `src/main/java/com/saveapenny/config/security/SecurityConfig.java` | https://docs.spring.io/spring-security/reference/ |
| JJWT | HS512 JWT creation and validation for access tokens | `pom.xml`, `src/main/java/com/saveapenny/auth/service/impl/JwtServiceImpl.java`, `src/main/resources/application.yml` | https://github.com/jwtk/jjwt#readme |
| springdoc OpenAPI | Generates OpenAPI spec and serves Swagger UI | `pom.xml`, `src/main/java/com/saveapenny/config/OpenApiConfig.java`, `src/main/resources/application.yml` | https://springdoc.org/ |

## Mapping, Logging, Metrics, And Background Work

| Technology | Used For In This Project | Evidence In Repo | Official Docs |
|------------|--------------------------|------------------|---------------|
| MapStruct | Compile-time DTO and entity mapping | `pom.xml`, `src/main/java/com/saveapenny/**/mapper/*.java` | https://mapstruct.org/documentation/stable/reference/html/ |
| Lombok | Reduces Java boilerplate in entities, DTOs, and support classes | `pom.xml`, usage across `src/main/java/com/saveapenny/**` | https://projectlombok.org/features/all |
| Spring Actuator | Health and metrics endpoints | `pom.xml`, `src/main/resources/application.yml` | https://docs.spring.io/spring-boot/reference/actuator/index.html |
| Micrometer | Application metrics instrumentation | `pom.xml`, metrics usage in analytics, stock, push, and admin modules | https://docs.micrometer.io/micrometer/reference/ |
| Prometheus Registry | Exposes metrics in Prometheus format | `pom.xml`, `src/main/resources/application.yml` | https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html |
| Logback | Main logging framework | `src/main/resources/logback-spring.xml` | https://logback.qos.ch/documentation.html |
| Logstash Logback Encoder | Structured JSON logs outside local profiles | `pom.xml`, `src/main/resources/logback-spring.xml` | https://github.com/logfellow/logstash-logback-encoder |
| Spring Scheduling | Cron-based recurring jobs for goals, insights, recurring transactions, and other timed work | `src/main/java/com/saveapenny/config/AsyncConfig.java`, scheduler classes under `src/main/java/com/saveapenny/**/scheduler/` | https://docs.spring.io/spring-framework/reference/integration/scheduling.html |
| Spring Async | Background execution for imports, OCR, push, and similar jobs | `src/main/java/com/saveapenny/config/AsyncConfig.java`, `@Async` usages across modules | https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async |
| Spring RestClient | Synchronous HTTP calls to external services | `pom.xml`, clients such as RevenueCat, FCM, and stock integrations | https://docs.spring.io/spring-framework/reference/integration/rest-clients.html |

## Optional Feature Integrations

| Technology / Service | Used For In This Project | Enabled By | Evidence In Repo | Official Docs |
|----------------------|--------------------------|------------|------------------|---------------|
| Spring AI | Shared AI integration layer for assistant and insight generation | Feature-specific flags | `pom.xml`, assistant and insight config classes | https://docs.spring.io/spring-ai/reference/ |
| OpenAI | Optional AI provider for assistant and insight flows | `ASSISTANT_ENABLED`, `OPENAI_API_KEY`, related insight settings | `src/main/resources/application.yml`, `src/main/java/com/saveapenny/assistant/config/AssistantAiConfig.java` | https://platform.openai.com/docs/overview |
| OpenRouter | Optional OpenAI-compatible provider for assistant and insight flows | `ASSISTANT_ENABLED`, `OPENROUTER_API_KEY`, related insight settings | `src/main/resources/application.yml`, assistant and insight config classes | https://openrouter.ai/docs |
| Tesseract OCR | OCR engine for receipt/document extraction | `OCR_ENABLED` | `Dockerfile`, `docker-compose.yml`, `src/main/java/com/saveapenny/ocr/**` | https://tesseract-ocr.github.io/tessdoc/ |
| Tess4J | Java binding used to call Tesseract from the app | `OCR_ENABLED` | `pom.xml`, `src/main/java/com/saveapenny/ocr/infrastructure/engine/tesseract/TesseractOcrService.java` | https://tess4j.sourceforge.net/ |
| Alpha Vantage | Stock market quotes, indicators, overview, and related market data | `STOCK_ENABLED`, `ALPHA_VANTAGE_API_KEY` | `src/main/resources/application.yml`, `src/main/java/com/saveapenny/stock/infrastructure/AlphaVantageClient.java` | https://www.alphavantage.co/documentation/ |
| RevenueCat | Subscription and entitlement sync for billing/plan enforcement | `REVENUECAT_ENABLED`, `REVENUECAT_SECRET_API_KEY` | `src/main/resources/application.yml`, `src/main/java/com/saveapenny/billing/**` | https://www.revenuecat.com/docs |
| Google Analytics 4 Measurement Protocol | Backend-published analytics events for mobile clients | `FIREBASE_ANALYTICS_ENABLED` | `src/main/resources/application.yml`, `src/main/java/com/saveapenny/analytics/**` | https://developers.google.com/analytics/devguides/collection/protocol/ga4 |
| Firebase Cloud Messaging | Push notification delivery through FCM HTTP v1 | `PUSH_FCM_ENABLED` | `src/main/resources/application.yml`, `src/main/java/com/saveapenny/push/**` | https://firebase.google.com/docs/cloud-messaging |

## Local Development And Deployment

| Technology | Used For In This Project | Evidence In Repo | Official Docs |
|------------|--------------------------|------------------|---------------|
| Docker | Container image build and runtime packaging | `Dockerfile`, `README.md` | https://docs.docker.com/ |
| Docker Compose | Local app + PostgreSQL orchestration | `docker-compose.yml`, `README.md`, `docs/getting-started.md` | https://docs.docker.com/compose/ |
| Eclipse Temurin | Java 24 base image for runtime container | `Dockerfile` | https://adoptium.net/temurin/ |

## Testing And Quality

| Technology | Used For In This Project | Evidence In Repo | Official Docs |
|------------|--------------------------|------------------|---------------|
| JUnit 5 | Main test framework | `pom.xml`, `src/test/java/**` | https://junit.org/junit5/docs/current/user-guide/ |
| Spring Test | MVC, JPA, and security testing support | `pom.xml` | https://docs.spring.io/spring-framework/reference/testing/ |
| Testcontainers | PostgreSQL-backed integration tests | `pom.xml`, tests using Testcontainers | https://java.testcontainers.org/ |
| H2 | Lightweight in-memory database for selected tests | `pom.xml` | https://www.h2database.com/html/main.html |
| JaCoCo | Test coverage reporting and coverage thresholds | `pom.xml` | https://www.jacoco.org/jacoco/trunk/doc/ |

## How The Stack Fits This Project

- Spring Boot, Spring Web MVC, and Spring Security provide the API and auth foundation.
- PostgreSQL, JPA/Hibernate, and Flyway back the core financial data model and schema lifecycle.
- MapStruct and Lombok keep the Java codebase maintainable as the number of modules grows.
- Actuator, Micrometer, Prometheus, Logback, and structured JSON logging support operations and monitoring.
- Scheduling and async execution power recurring transactions, goal progress checks, OCR jobs, notifications, and analytics.
- Optional integrations are feature-flagged so the core finance backend can run without AI or third-party commercial services.

## Related Project Docs

- [Architecture](architecture.md)
- [Getting Started](getting-started.md)
- [Deployment and Operations](deployment-operations.md)
- [Environment Variables Reference](env-reference.md)
- [Auth Flow](auth-flow.md)
- [Security](security.md)
- [Testing Guide](testing-guide.md)
- [Billing](features/billing.md)
- [Assistant](features/assistant.md)
- [OCR](features/ocr.md)
- [Stocks](features/stocks.md)
- [Firebase Analytics](features/firebase-analytics.md)
- [Notifications](features/notifications.md)
