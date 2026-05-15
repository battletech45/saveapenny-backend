package com.saveapenny.imports.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.imports.entity.Import;
import com.saveapenny.imports.entity.ImportRow;
import com.saveapenny.imports.entity.ImportRowStatus;
import com.saveapenny.imports.entity.ImportStatus;
import com.saveapenny.imports.repository.ImportRepository;
import com.saveapenny.imports.repository.ImportRowRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportAsyncJobServiceTest {

    @Mock
    private ImportRepository importRepository;

    @Mock
    private ImportRowRepository importRowRepository;

    @InjectMocks
    private ImportAsyncJobService importAsyncJobService;

    @Test
    void processImportAsync_marksDuplicateRowsAsSkipped() {
        UUID importId = UUID.randomUUID();
        Import importEntity = Import.builder()
                .id(importId)
                .status(ImportStatus.RUNNING)
                .totalRows(3)
                .build();

        ImportRow row1 = ImportRow.builder()
                .id(UUID.randomUUID())
                .importId(importId)
                .rowNumber(2)
                .rawData("EXPENSE,2026-05-01,25.00,USD,11111111-1111-1111-1111-111111111111,22222222-2222-2222-2222-222222222222,Coffee")
                .status(ImportRowStatus.VALID)
                .build();

        ImportRow row2 = ImportRow.builder()
                .id(UUID.randomUUID())
                .importId(importId)
                .rowNumber(3)
                .rawData("EXPENSE,2026-05-01,25.00,USD,11111111-1111-1111-1111-111111111111,33333333-3333-3333-3333-333333333333,Coffee")
                .status(ImportRowStatus.VALID)
                .build();

        ImportRow row3 = ImportRow.builder()
                .id(UUID.randomUUID())
                .importId(importId)
                .rowNumber(4)
                .rawData("EXPENSE,2026-05-02,10.00,USD,11111111-1111-1111-1111-111111111111,44444444-4444-4444-4444-444444444444,Snack")
                .status(ImportRowStatus.VALID)
                .build();

        when(importRepository.findById(importId)).thenReturn(Optional.of(importEntity));
        when(importRowRepository.findAllByImportIdOrderByRowNumberAsc(importId)).thenReturn(List.of(row1, row2, row3));

        importAsyncJobService.processImportAsync(importId);

        assertEquals(ImportRowStatus.IMPORTED, row1.getStatus());
        assertEquals(ImportRowStatus.SKIPPED, row2.getStatus());
        assertEquals("Duplicate transaction detected", row2.getErrorMessage());
        assertEquals(ImportRowStatus.IMPORTED, row3.getStatus());

        assertEquals(2, importEntity.getImportedRows());
        assertEquals(0, importEntity.getFailedRows());
        assertEquals(ImportStatus.COMPLETED, importEntity.getStatus());

        verify(importRowRepository).saveAll(any());
        verify(importRepository).save(importEntity);
    }
}
