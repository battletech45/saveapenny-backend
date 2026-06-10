package com.saveapenny.imports.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.imports.dto.ImportPreviewRowErrorResponse;
import com.saveapenny.imports.dto.ImportStatusResponse;
import com.saveapenny.imports.entity.Import;
import com.saveapenny.imports.entity.ImportRow;
import com.saveapenny.imports.entity.ImportRowStatus;
import com.saveapenny.imports.entity.ImportStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ImportMapperTest {

    private final ImportMapper importMapper = Mappers.getMapper(ImportMapper.class);

    @Test
    void toStatusResponse_mapsImportId() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Import entity = Import.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .fileName("test.csv")
                .status(ImportStatus.COMPLETED)
                .totalRows(10)
                .importedRows(8)
                .failedRows(2)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ImportStatusResponse response = importMapper.toStatusResponse(entity);

        assertEquals(id, response.getImportId());
        assertEquals(ImportStatus.COMPLETED, response.getStatus());
        assertEquals(10, response.getTotalRows());
        assertEquals(8, response.getImportedRows());
        assertEquals(2, response.getFailedRows());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    void toPreviewRowErrorResponse_mapsRow() {
        ImportRow row = ImportRow.builder()
                .id(UUID.randomUUID())
                .importId(UUID.randomUUID())
                .rowNumber(5)
                .rawData("a,b,c")
                .status(ImportRowStatus.FAILED)
                .errorMessage("Invalid amount")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ImportPreviewRowErrorResponse response = importMapper.toPreviewRowErrorResponse(row);

        assertEquals(5, response.getRowNumber());
        assertEquals("Invalid amount", response.getErrorMessage());
        assertEquals("a,b,c", response.getRawData());
    }
}
