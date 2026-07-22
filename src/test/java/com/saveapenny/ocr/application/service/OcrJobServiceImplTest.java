package com.saveapenny.ocr.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.billing.service.BillingAccessService;
import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.application.analysis.OcrAnalysisService;
import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysis;
import com.saveapenny.ocr.application.job.OcrJobAsyncProcessor;
import com.saveapenny.ocr.domain.exception.InvalidOcrFileException;
import com.saveapenny.ocr.domain.exception.OcrJobNotFoundException;
import com.saveapenny.ocr.domain.model.OcrJob;
import com.saveapenny.ocr.domain.model.OcrJobStatus;
import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import com.saveapenny.ocr.infrastructure.persistence.mapper.OcrJobMapper;
import com.saveapenny.ocr.infrastructure.persistence.repository.OcrJobRepository;
import com.saveapenny.ocr.interfaces.http.dto.OcrJobStatusResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrSubmitResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class OcrJobServiceImplTest {

    @Mock
    private OcrProperties ocrProperties;
    @Mock
    private OcrJobRepository ocrJobRepository;
    @Mock
    private OcrJobMapper ocrJobMapper;
    @Mock
    private OcrAnalysisService ocrAnalysisService;
    @Mock
    private OcrJobAsyncProcessor ocrJobAsyncProcessor;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private BillingAccessService billingAccessService;

    @Captor
    private ArgumentCaptor<OcrJob> jobCaptor;

    private OcrJobServiceImpl service;
    private UUID userId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new OcrJobServiceImpl(
                ocrProperties,
                ocrJobRepository,
                ocrJobMapper,
                ocrAnalysisService,
                ocrJobAsyncProcessor,
                transactionManager,
                billingAccessService);
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
    }

    @Test
    void createJob_whenDisabled_throws() {
        when(ocrProperties.enabled()).thenReturn(false);

        MultipartFile file = validFile();
        assertThrows(InvalidOcrFileException.class, () -> service.createJob(userId, file));
    }

    @Test
    void createJob_whenFileEmpty_throws() {
        when(ocrProperties.enabled()).thenReturn(true);

        MultipartFile empty = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
        assertThrows(InvalidOcrFileException.class, () -> service.createJob(userId, empty));
    }

    @Test
    void createJob_whenFileTooLarge_throws() {
        when(ocrProperties.enabled()).thenReturn(true);
        when(ocrProperties.maxFileSizeBytes()).thenReturn(10L);

        MultipartFile large = new MockMultipartFile("file", "large.png", "image/png", new byte[100]);
        assertThrows(InvalidOcrFileException.class, () -> service.createJob(userId, large));
    }

    @Test
    void createJob_whenUnsupportedContentType_throws() {
        when(ocrProperties.enabled()).thenReturn(true);
        when(ocrProperties.maxFileSizeBytes()).thenReturn(100_000L);

        MultipartFile gif = new MockMultipartFile("file", "test.gif", "image/gif", "data".getBytes());
        assertThrows(InvalidOcrFileException.class, () -> service.createJob(userId, gif));
    }

    @Test
    void createJob_whenNullContentType_throws() {
        when(ocrProperties.enabled()).thenReturn(true);
        when(ocrProperties.maxFileSizeBytes()).thenReturn(100_000L);

        MultipartFile noType = new MockMultipartFile("file", "test.png", null, "data".getBytes());
        assertThrows(InvalidOcrFileException.class, () -> service.createJob(userId, noType));
    }

    @Test
    void createJob_withValidPng_createsAndProcesses() {
        mockOcrEnabled();
        OcrJob savedJob = OcrJob.builder().id(jobId).status(OcrJobStatus.PENDING).build();
        when(ocrJobRepository.saveAndFlush(any())).thenReturn(savedJob);
        when(ocrJobMapper.toSubmitResponse(savedJob)).thenReturn(
                OcrSubmitResponse.builder().jobId(jobId).status(OcrJobStatus.PENDING).build());

        OcrSubmitResponse response = service.createJob(userId, validFile());

        assertNotNull(response);
        assertEquals(jobId, response.getJobId());
        assertEquals(OcrJobStatus.PENDING, response.getStatus());
        verify(ocrJobAsyncProcessor).process(any(), any());
    }

    @Test
    void createJob_withValidJpeg_createsAndProcesses() {
        mockOcrEnabled();
        OcrJob savedJob = OcrJob.builder().id(jobId).status(OcrJobStatus.PENDING).build();
        when(ocrJobRepository.saveAndFlush(any())).thenReturn(savedJob);
        when(ocrJobMapper.toSubmitResponse(savedJob)).thenReturn(
                OcrSubmitResponse.builder().jobId(jobId).status(OcrJobStatus.PENDING).build());

        MultipartFile jpeg = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "data".getBytes());
        OcrSubmitResponse response = service.createJob(userId, jpeg);

        assertNotNull(response);
        assertEquals(jobId, response.getJobId());
        verify(ocrJobRepository).saveAndFlush(jobCaptor.capture());
        assertEquals("image/jpeg", jobCaptor.getValue().getContentType());
    }

    @Test
    void createJob_withValidPdf_createsAndProcesses() {
        mockOcrEnabled();
        OcrJob savedJob = OcrJob.builder().id(jobId).status(OcrJobStatus.PENDING).build();
        when(ocrJobRepository.saveAndFlush(any())).thenReturn(savedJob);
        when(ocrJobMapper.toSubmitResponse(savedJob)).thenReturn(
                OcrSubmitResponse.builder().jobId(jobId).status(OcrJobStatus.PENDING).build());

        MultipartFile pdf = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "data".getBytes());
        OcrSubmitResponse response = service.createJob(userId, pdf);

        assertNotNull(response);
        assertEquals(jobId, response.getJobId());
        verify(ocrJobRepository).saveAndFlush(jobCaptor.capture());
        assertEquals("application/pdf", jobCaptor.getValue().getContentType());
    }

    @Test
    void createJob_setsUserIdAndFileName() {
        mockOcrEnabled();
        OcrJob savedJob = OcrJob.builder().id(jobId).status(OcrJobStatus.PENDING).build();
        when(ocrJobRepository.saveAndFlush(any())).thenReturn(savedJob);
        when(ocrJobMapper.toSubmitResponse(savedJob)).thenReturn(
                OcrSubmitResponse.builder().jobId(jobId).status(OcrJobStatus.PENDING).build());

        service.createJob(userId, validFile());

        verify(ocrJobRepository).saveAndFlush(jobCaptor.capture());
        OcrJob captured = jobCaptor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals("receipt.png", captured.getOriginalFileName());
        assertEquals("image/png", captured.getContentType());
        assertEquals(OcrJobStatus.PENDING, captured.getStatus());
    }

    @Test
    void getJobStatus_whenFound_returnsResponse() {
        OcrJob job = OcrJob.builder().id(jobId).userId(userId).status(OcrJobStatus.COMPLETED).rawText("some text").build();
        when(ocrJobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.of(job));

        OcrDocumentAnalysis analysis = new OcrDocumentAnalysis(
                List.of(), List.of(), "BANK_RECEIPT", "TRY", "Merchant",
                LocalDate.of(2026, 5, 20), null, List.of(), List.of(), List.of(), List.of(), 0.96,
                List.of(new OcrTransactionCandidate(LocalDate.of(2026, 5, 20), new BigDecimal("150.00"), "payment", null)));
        when(ocrAnalysisService.analyze("some text")).thenReturn(analysis);

        OcrJobStatusResponse mapperResponse = OcrJobStatusResponse.builder()
                .jobId(jobId).status(OcrJobStatus.COMPLETED).originalFileName("receipt.png").build();
        when(ocrJobMapper.toStatusResponse(job)).thenReturn(mapperResponse);
        when(ocrJobMapper.toTransactionCandidateResponses(any())).thenReturn(List.of());

        OcrJobStatusResponse result = service.getJobStatus(userId, jobId);

        assertEquals(jobId, result.getJobId());
        assertEquals(OcrJobStatus.COMPLETED, result.getStatus());
        assertEquals("BANK_RECEIPT", result.getDocumentType());
        assertEquals("TRY", result.getCurrency());
        assertEquals("Merchant", result.getMerchantName());
        verify(ocrAnalysisService).analyze("some text");
    }

    @Test
    void getJobStatus_whenNotFound_throws() {
        when(ocrJobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.empty());

        assertThrows(OcrJobNotFoundException.class, () -> service.getJobStatus(userId, jobId));
    }

    @Test
    void getJobStatus_withBlankText_noParseWarning() {
        OcrJob job = OcrJob.builder().id(jobId).userId(userId).status(OcrJobStatus.PENDING).rawText("").build();
        when(ocrJobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.of(job));

        OcrDocumentAnalysis emptyAnalysis = new OcrDocumentAnalysis(
                List.of(), List.of(), null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), 0.0, List.of());
        when(ocrAnalysisService.analyze("")).thenReturn(emptyAnalysis);

        OcrJobStatusResponse mapperResponse = OcrJobStatusResponse.builder()
                .jobId(jobId).status(OcrJobStatus.PENDING).build();
        when(ocrJobMapper.toStatusResponse(job)).thenReturn(mapperResponse);
        when(ocrJobMapper.toTransactionCandidateResponses(any())).thenReturn(List.of());

        OcrJobStatusResponse result = service.getJobStatus(userId, jobId);

        assertNull(result.getParseWarning());
    }

    @Test
    void normalizeFileName_whenNull_returnsDefault() {
        mockOcrEnabled();
        OcrJob savedJob = OcrJob.builder().id(jobId).status(OcrJobStatus.PENDING).build();
        when(ocrJobRepository.saveAndFlush(any())).thenReturn(savedJob);
        when(ocrJobMapper.toSubmitResponse(savedJob)).thenReturn(
                OcrSubmitResponse.builder().jobId(jobId).status(OcrJobStatus.PENDING).build());

        MultipartFile nullName = new MockMultipartFile("file", (String) null, "image/png", "data".getBytes());
        service.createJob(userId, nullName);

        verify(ocrJobRepository).saveAndFlush(jobCaptor.capture());
        assertEquals("ocr-upload", jobCaptor.getValue().getOriginalFileName());
    }

    @Test
    void createJob_whenIOException_throws() {
        mockOcrEnabled();
        OcrJob savedJob = OcrJob.builder().id(jobId).status(OcrJobStatus.PENDING).build();
        when(ocrJobRepository.saveAndFlush(any())).thenReturn(savedJob);

        MultipartFile broken = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes()) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("read error");
            }
        };

        assertThrows(InvalidOcrFileException.class, () -> service.createJob(userId, broken));
    }

    private void mockOcrEnabled() {
        when(ocrProperties.enabled()).thenReturn(true);
        when(ocrProperties.maxFileSizeBytes()).thenReturn(100_000L);
    }

    private MultipartFile validFile() {
        return new MockMultipartFile("file", "receipt.png", "image/png", "fake-image-data".getBytes());
    }
}
