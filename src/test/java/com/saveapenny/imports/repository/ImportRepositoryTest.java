package com.saveapenny.imports.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.imports.entity.Import;
import com.saveapenny.imports.entity.ImportStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ImportRepositoryTest {

    @Autowired
    private ImportRepository importRepository;

    private UUID userId;
    private Import entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entity = Import.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fileName("transactions.csv")
                .status(ImportStatus.COMPLETED)
                .totalRows(10)
                .importedRows(10)
                .failedRows(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        importRepository.save(entity);
    }

    @Test
    void findByIdAndUserId_returnsImport() {
        Optional<Import> found = importRepository.findByIdAndUserId(entity.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(entity.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        Optional<Import> found = importRepository.findByIdAndUserId(entity.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserId_returnsUserImports() {
        Page<Import> page = importRepository.findAllByUserId(userId, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserId_returnsEmptyForOtherUser() {
        Page<Import> page = importRepository.findAllByUserId(UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndStatus_filtersByStatus() {
        Page<Import> page = importRepository.findAllByUserIdAndStatus(userId, ImportStatus.COMPLETED, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndStatus_excludesWrongStatus() {
        Page<Import> page = importRepository.findAllByUserIdAndStatus(userId, ImportStatus.FAILED, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndStatus_returnsEmptyForOtherUser() {
        Page<Import> page = importRepository.findAllByUserIdAndStatus(UUID.randomUUID(), ImportStatus.PENDING, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }
}
