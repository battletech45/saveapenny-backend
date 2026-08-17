package com.saveapenny.creditcard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "credit_card_details")
public class CreditCardDetails {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditLimit;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal apr;

    @Column(name = "statement_day", nullable = false)
    private Integer statementDay;

    @Builder.Default
    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 21;

    @Column(name = "last_statement_date")
    private LocalDate lastStatementDate;

    @Column(name = "next_statement_date")
    private LocalDate nextStatementDate;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (gracePeriodDays == null) {
            gracePeriodDays = 21;
        }
        if (version == null) {
            version = 0L;
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
