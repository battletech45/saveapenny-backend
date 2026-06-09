package com.saveapenny.goal.repository;

import com.saveapenny.goal.entity.ScenarioEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<ScenarioEntity, UUID> {

    List<ScenarioEntity> findAllByGoalIdOrderByCreatedAtAsc(UUID goalId);

    Optional<ScenarioEntity> findByIdAndGoalId(UUID id, UUID goalId);

    Optional<ScenarioEntity> findByGoalIdAndIsBaselineTrue(UUID goalId);
}
