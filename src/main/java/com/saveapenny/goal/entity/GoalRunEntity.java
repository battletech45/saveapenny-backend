package com.saveapenny.goal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "goal_runs")
public class GoalRunEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "scenario_id", nullable = false)
    private UUID scenarioId;

    @Column(name = "inputs_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String inputsSnapshotJson;

    @Column(name = "output_summary_json", nullable = false, columnDefinition = "TEXT")
    private String outputSummaryJson;

    @Column(name = "output_series_json", columnDefinition = "TEXT")
    private String outputSeriesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Feasibility feasibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, length = 16)
    private GoalRunTrigger triggeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
