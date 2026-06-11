package com.saveapenny.imports.service.impl;

import com.saveapenny.imports.dto.ImportPreviewResponse;
import com.saveapenny.imports.dto.ImportPreviewRowErrorResponse;
import com.saveapenny.imports.dto.ImportStatusResponse;
import com.saveapenny.imports.entity.Import;
import com.saveapenny.imports.entity.ImportRow;
import com.saveapenny.imports.entity.ImportRowStatus;
import com.saveapenny.imports.entity.ImportStatus;
import com.saveapenny.imports.exception.ImportAlreadyRunningException;
import com.saveapenny.imports.exception.ImportNotFoundException;
import com.saveapenny.imports.exception.InvalidImportFileException;
import com.saveapenny.imports.mapper.ImportMapper;
import com.saveapenny.imports.repository.ImportRepository;
import com.saveapenny.imports.repository.ImportRowRepository;
import com.saveapenny.imports.service.ImportService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ImportServiceImpl implements ImportService {

    private final ImportRepository importRepository;
    private final ImportRowRepository importRowRepository;
    private final ImportMapper importMapper;
    private final ImportAsyncJobService importAsyncJobService;
    private final ImportRowParser importRowParser;

    public ImportServiceImpl(
            ImportRepository importRepository,
            ImportRowRepository importRowRepository,
            ImportMapper importMapper,
            ImportAsyncJobService importAsyncJobService,
            ImportRowParser importRowParser) {
        this.importRepository = importRepository;
        this.importRowRepository = importRowRepository;
        this.importMapper = importMapper;
        this.importAsyncJobService = importAsyncJobService;
        this.importRowParser = importRowParser;
    }

    @Override
    public ImportPreviewResponse preview(UUID currentUserId, MultipartFile file) {
        validateUpload(file);

        Import importEntity = Import.builder()
                .userId(currentUserId)
                .fileName(normalizeFileName(file.getOriginalFilename()))
                .status(ImportStatus.PENDING)
                .totalRows(0)
                .importedRows(0)
                .failedRows(0)
                .build();
        Import savedImport = importRepository.save(importEntity);

        List<ImportRow> rows = parseRows(savedImport.getId(), file);
        importRowRepository.saveAll(rows);

        int totalRows = rows.size();
        long invalidRows = rows.stream().filter(this::isInvalidRow).count();
        int failedRows = Math.toIntExact(invalidRows);
        int validRows = totalRows - failedRows;

        savedImport.setTotalRows(totalRows);
        savedImport.setImportedRows(0);
        savedImport.setFailedRows(failedRows);
        importRepository.save(savedImport);

        List<ImportPreviewRowErrorResponse> errors = rows.stream()
                .filter(this::isInvalidRow)
                .map(importMapper::toPreviewRowErrorResponse)
                .toList();

        return ImportPreviewResponse.builder()
                .importId(savedImport.getId())
                .fileName(savedImport.getFileName())
                .totalRows(totalRows)
                .validRows(validRows)
                .invalidRows(failedRows)
                .errors(errors)
                .build();
    }

    @Override
    public ImportStatusResponse confirm(UUID currentUserId, UUID importId) {
        Import importEntity = importRepository.findByIdAndUserIdForUpdate(importId, currentUserId)
                .orElseThrow(() -> new ImportNotFoundException(importId));
        if (importEntity.getStatus() == ImportStatus.RUNNING) {
            throw new ImportAlreadyRunningException(importId);
        }

        importEntity.setStatus(ImportStatus.RUNNING);
        importRepository.save(importEntity);

        importAsyncJobService.processImportAsync(importEntity.getId());

        return importMapper.toStatusResponse(importEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportStatusResponse getStatus(UUID currentUserId, UUID importId) {
        Import importEntity = findOwnedImport(currentUserId, importId);
        return importMapper.toStatusResponse(importEntity);
    }

    private Import findOwnedImport(UUID currentUserId, UUID importId) {
        return importRepository.findByIdAndUserId(importId, currentUserId)
                .orElseThrow(() -> new ImportNotFoundException(importId));
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImportFileException("file is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidImportFileException("file must be a CSV");
        }
    }

    private List<ImportRow> parseRows(UUID importId, MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || header.isBlank()) {
                throw new InvalidImportFileException("header row is missing");
            }

            List<ImportRow> rows = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                rows.add(buildImportRow(importId, rowNumber, line));
            }

            if (rows.isEmpty()) {
                throw new InvalidImportFileException("no data rows found");
            }

            return rows;
        } catch (IOException ex) {
            throw new InvalidImportFileException("unable to read file");
        }
    }

    private ImportRow buildImportRow(UUID importId, int rowNumber, String line) {
        String errorMessage = importRowParser.validate(line);
        ImportRowStatus status = errorMessage == null ? ImportRowStatus.VALID : ImportRowStatus.FAILED;

        return ImportRow.builder()
                .importId(importId)
                .rowNumber(rowNumber)
                .rawData(line)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    private boolean isInvalidRow(ImportRow row) {
        return row.getStatus() == ImportRowStatus.FAILED;
    }

    private String normalizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "transactions.csv";
        }
        return originalFileName.trim();
    }
}
