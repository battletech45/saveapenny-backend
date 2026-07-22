package com.saveapenny.ocr.interfaces.http.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.billing.entity.BillingEntitlement;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import com.saveapenny.billing.repository.BillingEntitlementRepository;
import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.support.runtime.OcrRuntimeEnvironment;
import com.saveapenny.ocr.support.runtime.OcrRuntimeChecker;
import com.saveapenny.ocr.support.runtime.OcrRuntimeStatus;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ocr-real-media;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "ocr.enabled=true",
        "ocr.max-file-size-bytes=20000000"
})
class OcrImportRealMediaIntegrationTest {

    private static final String TESSDATA_PATH = OcrRuntimeEnvironment.resolveTessdataPath(null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OcrProperties ocrProperties;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BillingEntitlementRepository billingEntitlementRepository;

    @BeforeAll
    static void preloadNativeLibraryPath() {
        OcrRuntimeEnvironment.configureNativeLibraryPathIfMissing();
        Assumptions.assumeTrue(OcrRuntimeEnvironment.canLoadNativeTesseract());
    }

    @DynamicPropertySource
    static void configureOcrProperties(DynamicPropertyRegistry registry) {
        if (TESSDATA_PATH != null) {
            registry.add("ocr.tessdata-path", () -> TESSDATA_PATH);
        }
    }

    @BeforeEach
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @TestConfiguration
    static class RuntimeCheckerConfig {

        @Bean
        @Primary
        OcrRuntimeChecker ocrRuntimeChecker() {
            OcrRuntimeChecker checker = mock(OcrRuntimeChecker.class);
            when(checker.check()).thenReturn(new OcrRuntimeStatus(true, true, true, "eng", TESSDATA_PATH == null ? "/tmp" : TESSDATA_PATH, null));
            return checker;
        }
    }

    @Test
    void uploadPng_extractsAndParsesCandidate() throws Exception {
        String token = registerAndGetToken("ocr.real.png@example.com", "OCR Real PNG");
        MockMultipartFile file = createPngReceipt();

        MvcResult result = submitAndAwaitCompletion(token, file);
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");

        assertTrue(data.path("rawText").asText().length() > 0);
        assertTrue(data.path("transactionCandidates").isArray());
        assertTrue(data.path("transactionCandidates").size() >= 1);
        assertTrue(hasCandidate(data.path("transactionCandidates"), "2026-05-20", new BigDecimal("15.50")));
    }

    @Test
    void uploadPdf_extractsAndParsesCandidate() throws Exception {
        String token = registerAndGetToken("ocr.real.pdf@example.com", "OCR Real PDF");
        MockMultipartFile file = createPdfReceipt();

        MvcResult result = submitAndAwaitCompletion(token, file);
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");

        assertTrue(data.path("rawText").asText().length() > 0);
        assertTrue(data.path("transactionCandidates").isArray());
        assertTrue(data.path("transactionCandidates").size() >= 1);
        assertTrue(hasCandidate(data.path("transactionCandidates"), "2026-05-21", new BigDecimal("19.99")));
    }

    private MvcResult submitAndAwaitCompletion(String token, MockMultipartFile file) throws Exception {
        MvcResult uploadResult = mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId = extractField(uploadResult, "data", "jobId");
        int attempts = Math.max(1, ocrProperties.maxRetries() + 1);
        long timeoutBudgetMillis = Math.max(15_000L, (ocrProperties.jobTimeoutMillis() * attempts) + 10_000L);
        long deadlineMillis = System.currentTimeMillis() + timeoutBudgetMillis;
        String lastStatus = "PENDING";

        while (System.currentTimeMillis() < deadlineMillis) {
            Thread.sleep(200);
            MvcResult statusResult = mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            lastStatus = extractField(statusResult, "data", "status");
            if ("COMPLETED".equals(lastStatus)) {
                return statusResult;
            }
            if ("FAILED".equals(lastStatus)) {
                throw new AssertionError("OCR job failed: " + statusResult.getResponse().getContentAsString());
            }
        }

        throw new AssertionError("OCR job did not reach COMPLETED in time within "
                + timeoutBudgetMillis + " ms; last status=" + lastStatus + ", attempts=" + attempts);
    }

    private MockMultipartFile createPngReceipt() throws Exception {
        BufferedImage image = new BufferedImage(1400, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Serif", Font.BOLD, 72));
        graphics.drawString("MARKET", 80, 140);
        graphics.drawString("2026-05-20 market 15.50", 80, 280);
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("file", "receipt.png", "image/png", baos.toByteArray());
    }

    private MockMultipartFile createPdfReceipt() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 28);
                stream.newLineAtOffset(80, 700);
                stream.showText("RECEIPT");
                stream.endText();

                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 24);
                stream.newLineAtOffset(80, 650);
                stream.showText("2026-05-21 market 19.99");
                stream.endText();
            }

            document.save(baos);
            return new MockMultipartFile("file", "receipt.pdf", "application/pdf", baos.toByteArray());
        }
    }

    private String registerAndGetToken(String email, String fullName) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Strong@123",
                  "fullName": "%s"
                }
                """.formatted(email, fullName);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registerJson.path("data").path("accessToken").asText();

        billingEntitlementRepository.save(BillingEntitlement.builder()
                .userId(jwtService.extractUserId(token))
                .plan(Plan.PLUS)
                .status(EntitlementStatus.ACTIVE)
                .willRenew(true)
                .build());

        return token;
    }

    private String extractField(MvcResult result, String objectName, String fieldName) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path(objectName).path(fieldName).asText();
    }

    private boolean hasCandidate(JsonNode candidates, String date, BigDecimal amount) {
        for (JsonNode candidate : candidates) {
            if (!date.equals(candidate.path("date").asText())) {
                continue;
            }
            try {
                BigDecimal actual = new BigDecimal(candidate.path("amount").asText());
                if (actual.compareTo(amount) == 0) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                continue;
            }
        }
        return false;
    }

}
