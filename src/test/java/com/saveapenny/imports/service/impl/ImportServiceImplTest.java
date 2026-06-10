package com.saveapenny.imports.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.imports.dto.ImportPreviewResponse;
import com.saveapenny.imports.dto.ImportStatusResponse;
import com.saveapenny.imports.entity.Import;
import com.saveapenny.imports.entity.ImportStatus;
import com.saveapenny.imports.exception.ImportAlreadyRunningException;
import com.saveapenny.imports.exception.ImportNotFoundException;
import com.saveapenny.imports.exception.InvalidImportFileException;
import com.saveapenny.imports.mapper.ImportMapper;
import com.saveapenny.imports.repository.ImportRepository;
import com.saveapenny.imports.repository.ImportRowRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImportServiceImplTest {

    @Mock
    private ImportRepository importRepository;
    @Mock
    private ImportRowRepository importRowRepository;
    @Mock
    private ImportMapper importMapper;
    @Mock
    private ImportAsyncJobService importAsyncJobService;

    @InjectMocks
    private ImportServiceImpl importService;

    private UUID userId;
    private UUID importId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        importId = UUID.randomUUID();
    }

    @Test
    void preview_returnsSummary_withValidAndInvalidRows() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                ("type,date,amount,currency,accountId,categoryId\n"
                        + "EXPENSE,2026-05-01,12.50,USD,a1,c1\n"
                        + "EXPENSE,2026-05-02,not-a-number,USD,a2,c2\n")
                        .getBytes());

        Import savedImport = Import.builder()
                .id(importId)
                .userId(userId)
                .fileName("transactions.csv")
                .status(ImportStatus.PENDING)
                .totalRows(0)
                .importedRows(0)
                .failedRows(0)
                .build();

        when(importRepository.save(any(Import.class))).thenReturn(savedImport);

        ImportPreviewResponse response = importService.preview(userId, file);

        assertEquals(importId, response.getImportId());
        assertEquals(2, response.getTotalRows());
        assertEquals(1, response.getValidRows());
        assertEquals(1, response.getInvalidRows());
        verify(importRowRepository).saveAll(anyList());
        verify(importRepository, times(2)).save(any(Import.class));
    }

    @Test
    void preview_throws_whenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "transactions.csv", "text/csv", new byte[0]);

        assertThrows(InvalidImportFileException.class, () -> importService.preview(userId, file));
        verify(importRepository, never()).save(any(Import.class));
    }

    @Test
    void confirm_setsRunningAndDispatchesAsync() {
        Import importEntity = Import.builder()
                .id(importId)
                .userId(userId)
                .status(ImportStatus.PENDING)
                .build();
        ImportStatusResponse mapped = ImportStatusResponse.builder()
                .importId(importId)
                .status(ImportStatus.RUNNING)
                .build();

        when(importRepository.findByIdAndUserId(importId, userId)).thenReturn(Optional.of(importEntity));
        when(importRepository.save(any(Import.class))).thenReturn(importEntity);
        when(importMapper.toStatusResponse(importEntity)).thenReturn(mapped);

        ImportStatusResponse response = importService.confirm(userId, importId);

        assertEquals(ImportStatus.RUNNING, response.getStatus());
        verify(importAsyncJobService).processImportAsync(importId);
    }

    @Test
    void confirm_throws_whenAlreadyRunning() {
        Import importEntity = Import.builder().id(importId).userId(userId).status(ImportStatus.RUNNING).build();
        when(importRepository.findByIdAndUserId(importId, userId)).thenReturn(Optional.of(importEntity));

        assertThrows(ImportAlreadyRunningException.class, () -> importService.confirm(userId, importId));
        verify(importAsyncJobService, never()).processImportAsync(any(UUID.class));
    }

    @Test
    void getStatus_returnsResponse_whenFound() {
        Import importEntity = Import.builder()
                .id(importId)
                .userId(userId)
                .status(ImportStatus.COMPLETED)
                .totalRows(5)
                .importedRows(5)
                .failedRows(0)
                .build();
        ImportStatusResponse mapped = ImportStatusResponse.builder()
                .importId(importId)
                .status(ImportStatus.COMPLETED)
                .totalRows(5)
                .importedRows(5)
                .failedRows(0)
                .build();

        when(importRepository.findByIdAndUserId(importId, userId)).thenReturn(Optional.of(importEntity));
        when(importMapper.toStatusResponse(importEntity)).thenReturn(mapped);

        ImportStatusResponse result = importService.getStatus(userId, importId);

        assertEquals(ImportStatus.COMPLETED, result.getStatus());
        assertEquals(5, result.getImportedRows());
    }

    @Test
    void getStatus_throws_whenNotFound() {
        when(importRepository.findByIdAndUserId(importId, userId)).thenReturn(Optional.empty());

        assertThrows(ImportNotFoundException.class, () -> importService.getStatus(userId, importId));
    }
}
