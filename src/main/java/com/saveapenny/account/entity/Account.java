package com.saveapenny.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
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
@Table(name = "accounts")
public class Account {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "initial_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        if (initialBalance == null) {
            initialBalance = BigDecimal.ZERO;
        }

        if (active == null) {
            active = true;
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
