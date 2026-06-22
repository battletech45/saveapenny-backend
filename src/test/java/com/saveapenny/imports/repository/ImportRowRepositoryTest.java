package com.saveapenny.imports.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.imports.entity.ImportRow;
import com.saveapenny.imports.entity.ImportRowStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ImportRowRepositoryTest {

    @Autowired
    private ImportRowRepository importRowRepository;

    private UUID importId;
    private ImportRow row1;
    private ImportRow row2;
    private ImportRow row3;

    @BeforeEach
    void setUp() {
        importId = UUID.randomUUID();

        row1 = ImportRow.builder()
                .id(UUID.randomUUID())
                .importId(importId)
                .rowNumber(1)
                .rawData("{\"date\":\"2026-01-01\",\"amount\":100}")
                .status(ImportRowStatus.IMPORTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        row2 = ImportRow.builder()
                .id(UUID.randomUUID())
                .importId(importId)
                .rowNumber(2)
                .rawData("{\"date\":\"2026-01-02\",\"amount\":200}")
                .status(ImportRowStatus.IMPORTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        row3 = ImportRow.builder()
                .id(UUID.randomUUID())
                .importId(importId)
                .rowNumber(3)
                .rawData("{\"date\":\"2026-01-03\",\"amount\":-1}")
                .status(ImportRowStatus.FAILED)
                .errorMessage("Invalid amount")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        importRowRepository.save(row1);
        importRowRepository.save(row2);
        importRowRepository.save(row3);
    }

    @Test
    void findAllByImportIdOrderByRowNumberAsc_returnsAllRowsOrdered() {
        List<ImportRow> rows = importRowRepository.findAllByImportIdOrderByRowNumberAsc(importId);
        assertEquals(3, rows.size());
        assertEquals(1, rows.get(0).getRowNumber());
        assertEquals(2, rows.get(1).getRowNumber());
        assertEquals(3, rows.get(2).getRowNumber());
    }

    @Test
    void findAllByImportIdOrderByRowNumberAsc_returnsEmptyForUnknownImport() {
        List<ImportRow> rows = importRowRepository.findAllByImportIdOrderByRowNumberAsc(UUID.randomUUID());
        assertTrue(rows.isEmpty());
    }

    @Test
    void countByImportIdAndStatus_countsMatchingRows() {
        long imported = importRowRepository.countByImportIdAndStatus(importId, ImportRowStatus.IMPORTED);
        assertEquals(2, imported);

        long failed = importRowRepository.countByImportIdAndStatus(importId, ImportRowStatus.FAILED);
        assertEquals(1, failed);
    }

    @Test
    void countByImportIdAndStatus_returnsZeroForNonMatchingStatus() {
        long skipped = importRowRepository.countByImportIdAndStatus(importId, ImportRowStatus.SKIPPED);
        assertEquals(0, skipped);
    }

    @Test
    void existsByImportIdAndRowNumber_returnsTrueWhenExists() {
        assertTrue(importRowRepository.existsByImportIdAndRowNumber(importId, 1));
    }

    @Test
    void existsByImportIdAndRowNumber_returnsFalseWhenNotExists() {
        assertFalse(importRowRepository.existsByImportIdAndRowNumber(importId, 99));
    }
}
