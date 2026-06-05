package com.saveapenny.ocr.application.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.application.port.in.OcrService;
import com.saveapenny.ocr.application.port.in.OcrUploadPayload;
import com.saveapenny.ocr.domain.model.OcrJob;
import com.saveapenny.ocr.domain.model.OcrJobStatus;
import com.saveapenny.ocr.infrastructure.persistence.repository.OcrJobRepository;
import com.saveapenny.ocr.support.monitoring.OcrMetrics;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OcrJobAsyncProcessorTest {

    @Test
    void process_marksJobFailedWhenErrorEscapesOcrEngine() {
        OcrJobRepository repository = org.mockito.Mockito.mock(OcrJobRepository.class);
        OcrService ocrService = org.mockito.Mockito.mock(OcrService.class);
        OcrMetrics metrics = org.mockito.Mockito.mock(OcrMetrics.class);
        OcrJobAsyncProcessor processor = new OcrJobAsyncProcessor(
                repository,
                ocrService,
                new OcrProperties(true, "/tmp", "eng", 3, 1024, 1000, 0, false),
                metrics);

        UUID jobId = UUID.randomUUID();
        OcrJob job = OcrJob.builder()
                .id(jobId)
                .userId(UUID.randomUUID())
                .originalFileName("receipt.pdf")
                .contentType("application/pdf")
                .status(OcrJobStatus.PENDING)
                .build();

        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        when(repository.save(any(OcrJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ocrService.extractTextAsync(any())).thenThrow(new UnsatisfiedLinkError("Unable to load library 'tesseract'"));

        processor.process(jobId, new OcrUploadPayload("receipt.pdf", "application/pdf", "pdf-bytes".getBytes()));

        ArgumentCaptor<OcrJob> captor = ArgumentCaptor.forClass(OcrJob.class);
        verify(repository, times(2)).save(captor.capture());
        OcrJob failedJob = captor.getAllValues().getLast();
        assertEquals(OcrJobStatus.FAILED, failedJob.getStatus());
        assertEquals("Unable to load library 'tesseract'", failedJob.getErrorMessage());
        verify(metrics).markFailure();
    }
}
