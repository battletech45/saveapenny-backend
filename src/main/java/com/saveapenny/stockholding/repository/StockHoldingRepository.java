package com.saveapenny.stockholding.repository;

import com.saveapenny.stockholding.entity.StockHolding;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockHoldingRepository extends JpaRepository<StockHolding, UUID> {

    Optional<StockHolding> findByIdAndUserId(UUID id, UUID userId);

    Page<StockHolding> findAllByUserId(UUID userId, Pageable pageable);

    List<StockHolding> findAllByUserId(UUID userId);

    List<StockHolding> findAllByUserIdAndSymbol(UUID userId, String symbol);

    boolean existsByUserIdAndSymbolAndPurchaseDate(UUID userId, String symbol, LocalDate purchaseDate);

    @Modifying
    @Query("DELETE FROM StockHolding h WHERE h.id = :id AND h.userId = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
