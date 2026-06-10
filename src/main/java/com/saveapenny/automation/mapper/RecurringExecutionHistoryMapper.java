package com.saveapenny.automation.mapper;

import com.saveapenny.automation.dto.RecurringExecutionHistoryResponse;
import com.saveapenny.automation.entity.RecurringExecutionHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecurringExecutionHistoryMapper {

    RecurringExecutionHistoryResponse toResponse(RecurringExecutionHistory history);
}
