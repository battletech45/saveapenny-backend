package com.saveapenny.goal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "goal_scenarios")
public class ScenarioEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "inputs_json", nullable = false, columnDefinition = "TEXT")
    private String inputsJson;

    @Column(name = "is_baseline", nullable = false)
    private Boolean isBaseline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (isBaseline == null) {
            isBaseline = false;
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
