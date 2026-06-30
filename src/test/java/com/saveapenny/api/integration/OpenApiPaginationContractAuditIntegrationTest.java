package com.saveapenny.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapi-pagination-audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class OpenApiPaginationContractAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final List<String> EXPECTED_PAGINATED_PATHS = List.of(
            "/paths/~1api~1v1~1accounts/get",
            "/paths/~1api~1v1~1transactions/get",
            "/paths/~1api~1v1~1budgets/get",
            "/paths/~1api~1v1~1notifications/get",
            "/paths/~1api~1v1~1goals/get",
            "/paths/~1api~1v1~1goals~1{goalId}~1runs/get",
            "/paths/~1api~1v1~1automations~1recurring-transactions/get",
            "/paths/~1api~1v1~1automations~1recurring-transactions~1{recurringTransactionId}~1history/get",
            "/paths/~1api~1v1~1audits/get",
            "/paths/~1api~1v1~1stocks~1holdings/get",
            "/paths/~1api~1v1~1insights/get");

    private static final List<String> EXPECTED_PAGINATED_RESPONSE_FIELDS = List.of(
            "items", "page", "size", "totalItems", "totalPages", "hasNext", "hasPrevious");

    private static final List<String> SPRING_PAGE_FIELDS = List.of(
            "content", "pageable", "sort", "first", "last", "empty",
            "number", "numberOfElements", "totalElements");

    @Test
    void openApi_exposesExpectedPaginatedPaths() throws Exception {
        JsonNode root = openApiRoot();

        for (String path : EXPECTED_PAGINATED_PATHS) {
            assertThat(root.at(path).isMissingNode())
                    .as("Expected paginated path %s to exist", path)
                    .isFalse();
        }
    }

    @Test
    void openApi_usesSharedPagedResponseSchemasForAllListEndpoints() throws Exception {
        JsonNode root = openApiRoot();

        for (String path : EXPECTED_PAGINATED_PATHS) {
            assertThat(operationNode(root, path))
                    .as("Paginated endpoint at %s must reference PagedResponse", path)
                    .contains("PagedResponse");
        }
    }

    @Test
    void openApi_pagedResponseConcreteSchemasContainExpectedFields() throws Exception {
        JsonNode schemas = openApiRoot().get("components").get("schemas");

        List<String> pagedResponseSchemas = streamOf(schemas.fieldNames())
                .filter(name -> name.startsWith("PagedResponse"))
                .toList();

        assertThat(pagedResponseSchemas)
                .as("At least one PagedResponse* schema must exist")
                .isNotEmpty();

        for (String schemaName : pagedResponseSchemas) {
            JsonNode properties = schemas.get(schemaName).get("properties");
            assertThat(properties)
                    .as("Schema %s must have properties", schemaName)
                    .isNotNull();

            for (String field : EXPECTED_PAGINATED_RESPONSE_FIELDS) {
                assertThat(properties.has(field))
                        .as("Schema %s must contain field '%s'", schemaName, field)
                        .isTrue();
            }
        }
    }

    @Test
    void openApi_noPagedResponseSchemaContainsSpringPageFields() throws Exception {
        JsonNode schemas = openApiRoot().get("components").get("schemas");

        List<String> pagedResponseSchemas = streamOf(schemas.fieldNames())
                .filter(name -> name.startsWith("PagedResponse"))
                .toList();

        for (String schemaName : pagedResponseSchemas) {
            JsonNode properties = schemas.get(schemaName).get("properties");
            for (String springField : SPRING_PAGE_FIELDS) {
                assertThat(properties.has(springField))
                        .as("PagedResponse schema %s must not contain Spring field '%s'",
                                schemaName, springField)
                        .isFalse();
            }
        }
    }

    @Test
    void openApi_noOperationLeaksRawPageImplReference() throws Exception {
        JsonNode root = openApiRoot();

        for (String path : EXPECTED_PAGINATED_PATHS) {
            String opJson = operationNode(root, path);
            assertThat(opJson)
                    .as("Endpoint at %s must not reference Spring PageImpl", path)
                    .doesNotContain("PageImpl");
            assertThat(opJson)
                    .as("Endpoint at %s must reference PagedResponse, not raw Page", path)
                    .contains("PagedResponse");
        }
    }

    private JsonNode openApiRoot() throws Exception {
        String response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String operationNode(JsonNode root, String jsonPointer) throws Exception {
        return objectMapper.writeValueAsString(root.at(jsonPointer));
    }

    private static java.util.stream.Stream<String> streamOf(Iterator<String> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
