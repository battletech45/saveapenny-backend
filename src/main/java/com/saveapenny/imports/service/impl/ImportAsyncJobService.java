package com.saveapenny.imports.service.impl;

import com.saveapenny.imports.entity.Import;
import com.saveapenny.imports.entity.ImportRow;
import com.saveapenny.imports.entity.ImportRowStatus;
import com.saveapenny.imports.entity.ImportStatus;
import com.saveapenny.imports.repository.ImportRepository;
import com.saveapenny.imports.repository.ImportRowRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportAsyncJobService {

    private static final int DATE_INDEX = 1;
    private static final int AMOUNT_INDEX = 2;
    private static final int ACCOUNT_ID_INDEX = 4;
    private static final int DESCRIPTION_INDEX = 6;

    private final ImportRepository importRepository;
    private final ImportRowRepository importRowRepository;

    public ImportAsyncJobService(ImportRepository importRepository, ImportRowRepository importRowRepository) {
        this.importRepository = importRepository;
        this.importRowRepository = importRowRepository;
    }

    @Async("importTaskExecutor")
    @Transactional
    public void processImportAsync(UUID importId) {
        Import importEntity = importRepository.findById(importId).orElse(null);
        if (importEntity == null) {
            return;
        }

        try {
            List<ImportRow> rows = importRowRepository.findAllByImportIdOrderByRowNumberAsc(importId);
            Set<String> seenHashes = new HashSet<>();

            int importedRows = 0;
            int failedRows = 0;
            for (ImportRow row : rows) {
                if (row.getStatus() == ImportRowStatus.FAILED) {
                    failedRows++;
                    continue;
                }

                String transactionHash = computeTransactionHash(row.getRawData());
                if (!seenHashes.add(transactionHash)) {
                    row.setStatus(ImportRowStatus.SKIPPED);
                    row.setErrorMessage("Duplicate transaction detected");
                    continue;
                }

                row.setStatus(ImportRowStatus.IMPORTED);
                row.setErrorMessage(null);
                importedRows++;
            }

            importRowRepository.saveAll(rows);

            importEntity.setImportedRows(importedRows);
            importEntity.setFailedRows(failedRows);
            importEntity.setStatus(ImportStatus.COMPLETED);
            importRepository.save(importEntity);
        } catch (RuntimeException ex) {
            importEntity.setStatus(ImportStatus.FAILED);
            importRepository.save(importEntity);
            throw ex;
        }
    }

    private String computeTransactionHash(String rawData) {
        String[] parts = rawData == null ? new String[0] : rawData.split(",", -1);

        String accountId = safe(parts, ACCOUNT_ID_INDEX).trim();
        String amount = safe(parts, AMOUNT_INDEX).trim();
        String date = normalizeDate(safe(parts, DATE_INDEX).trim());
        String description = safe(parts, DESCRIPTION_INDEX).trim().toLowerCase(Locale.ROOT);

        String payload = String.join("|", accountId, amount, date, description);
        return sha256Hex(payload);
    }

    private String safe(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    private String normalizeDate(String date) {
        try {
            return LocalDate.parse(date).toString();
        } catch (RuntimeException ex) {
            return date;
        }
    }

    private String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
