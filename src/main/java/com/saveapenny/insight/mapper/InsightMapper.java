package com.saveapenny.insight.mapper;

import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.dto.InsightResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InsightMapper {

    InsightResponse toResponse(InsightEntity entity);
}
