package com.saveapenny.ocr.application.job;

import com.saveapenny.ocr.application.port.in.OcrService;
import com.saveapenny.ocr.application.port.in.OcrUploadPayload;
import com.saveapenny.ocr.domain.exception.OcrProcessingException;
import com.saveapenny.ocr.domain.model.OcrJob;
import com.saveapenny.ocr.domain.model.OcrJobStatus;
import com.saveapenny.ocr.infrastructure.persistence.repository.OcrJobRepository;
import com.saveapenny.ocr.support.monitoring.OcrMetrics;
import com.saveapenny.config.OcrProperties;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OcrJobAsyncProcessor {

    private static final Logger log = LoggerFactory.getLogger(OcrJobAsyncProcessor.class);

    private final OcrJobRepository ocrJobRepository;
    private final OcrService ocrService;
    private final OcrProperties ocrProperties;
    private final OcrMetrics ocrMetrics;

    public OcrJobAsyncProcessor(
            OcrJobRepository ocrJobRepository,
            OcrService ocrService,
            OcrProperties ocrProperties,
            OcrMetrics ocrMetrics) {
        this.ocrJobRepository = ocrJobRepository;
        this.ocrService = ocrService;
        this.ocrProperties = ocrProperties;
        this.ocrMetrics = ocrMetrics;
    }

    @Async("ocrTaskExecutor")
    public void process(UUID jobId, OcrUploadPayload file) {
        long startedAt = System.nanoTime();
        OcrJob job = ocrJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        try {
            job.setStatus(OcrJobStatus.RUNNING);
            ocrJobRepository.save(job);

            String rawText = extractWithRetry(file);
            job.setRawText(rawText);
            job.setResultSnippet(toSnippet(rawText));
            job.setErrorMessage(null);
            job.setStatus(OcrJobStatus.COMPLETED);
            ocrJobRepository.save(job);
            long elapsed = System.nanoTime() - startedAt;
            ocrMetrics.markSuccess();
            ocrMetrics.recordDuration(elapsed);
            log.info("ocr_job_completed jobId={} result=success durationMs={}",
                    job.getId(), TimeUnit.NANOSECONDS.toMillis(elapsed));
            if (ocrProperties.debugLogging()) {
                log.debug("ocr_job_raw_text jobId={} rawText={}", job.getId(), mask(rawText));
            }
        } catch (Throwable ex) {
            job.setStatus(OcrJobStatus.FAILED);
            job.setErrorMessage(truncate(resolveMessage(ex), 240));
            ocrJobRepository.save(job);
            long elapsed = System.nanoTime() - startedAt;
            ocrMetrics.markFailure();
            ocrMetrics.recordDuration(elapsed);
            log.warn("ocr_job_failed jobId={} result=failure durationMs={} message={}",
                    job.getId(), TimeUnit.NANOSECONDS.toMillis(elapsed), resolveMessage(ex));
        }
    }

    private String extractWithRetry(OcrUploadPayload file) {
        RuntimeException last = null;
        int attempts = Math.max(1, ocrProperties.maxRetries() + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                var future = ocrService.extractTextAsync(file);
                if (future == null) {
                    return ocrService.extractText(file);
                }
                return future.orTimeout(ocrProperties.jobTimeoutMillis(), TimeUnit.MILLISECONDS).join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                last = cause instanceof RuntimeException runtime ? runtime : new OcrProcessingException("OCR failed", cause);
            } catch (RuntimeException ex) {
                last = ex;
            }

            if (!isTransient(last) || attempt == attempts) {
                break;
            }
            log.info("ocr_job_retry attempt={} maxAttempts={}", attempt + 1, attempts);
        }
        throw last == null ? new OcrProcessingException("OCR failed") : last;
    }

    private boolean isTransient(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("timeout") || normalized.contains("tempor") || normalized.contains("unable to read");
    }

    private String mask(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 30 ? "***" : normalized.substring(0, 10) + "***" + normalized.substring(normalized.length() - 10);
    }

    private String toSnippet(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 250 ? normalized : normalized.substring(0, 250);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String resolveMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return throwable.getClass().getSimpleName();
    }
}
