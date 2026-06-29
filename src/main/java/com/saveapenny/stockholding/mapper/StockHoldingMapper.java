package com.saveapenny.stockholding.mapper;

import com.saveapenny.stockholding.dto.CreateHoldingRequest;
import com.saveapenny.stockholding.dto.HoldingResponse;
import com.saveapenny.stockholding.dto.UpdateHoldingRequest;
import com.saveapenny.stockholding.entity.StockHolding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface StockHoldingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    StockHolding toEntity(CreateHoldingRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "symbol", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "purchasePrice", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "quantity", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "purchaseDate", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "notes", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget StockHolding entity, UpdateHoldingRequest request);

    @Mapping(target = "investedAmount", ignore = true)
    @Mapping(target = "currentPrice", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "profitLoss", ignore = true)
    @Mapping(target = "profitLossPercent", ignore = true)
    @Mapping(target = "latestTradingDay", ignore = true)
    HoldingResponse toResponse(StockHolding entity);
}
