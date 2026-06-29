package com.saveapenny.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI saveAPennyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SaveAPenny API")
                        .version("v1")
                        .description("""
                                SaveAPenny backend API for personal finance workflows.

                                Quick start:
                                1) Register or login from Auth endpoints.
                                2) Click Authorize and paste your JWT as: Bearer <token>.
                                3) Create account/category resources first, then transactions and budgets.
                                4) Use import endpoints for CSV and OCR ingestion flows.

                                Notes:
                                - All secured endpoints require a JWT unless stated otherwise.
                                - Date/time fields use ISO-8601 formats.
                                - Paginated endpoints return metadata through Spring page structures.
                                """)
                        .contact(new Contact()
                                .name("SaveAPenny API Support")
                                .email("support@saveapenny.local"))
                        .license(new License()
                                .name("Internal Use")
                                .url("https://example.com/internal")))
                .addTagsItem(new Tag().name("Auth").description("Register, login, and token-based access."))
                .addTagsItem(new Tag().name("Accounts").description("Manage user accounts and balances."))
                .addTagsItem(new Tag().name("Categories").description("Manage transaction categories."))
                .addTagsItem(new Tag().name("Transactions").description("Create, update, and query transactions."))
                .addTagsItem(new Tag().name("Budgets").description("Create and monitor budget goals."))
                .addTagsItem(new Tag().name("Reports").description("Generate reporting summaries and exports."))
                .addTagsItem(new Tag().name("Imports").description("Import transactional data from files."))
                .addTagsItem(new Tag().name("OCR Imports").description("Upload receipt image/PDF and poll OCR job status."))
                .addTagsItem(new Tag().name("Automation").description("Recurring transaction schedules and runs."))
                .addTagsItem(new Tag().name("Notifications").description("Notification channels and delivery endpoints."))
                .addTagsItem(new Tag().name("Audit").description("Read audit trail events and history."))
                .addTagsItem(new Tag().name("Admin").description("Administrative observability and metrics endpoints."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste token as: Bearer <jwt-token>")));
    }
}
