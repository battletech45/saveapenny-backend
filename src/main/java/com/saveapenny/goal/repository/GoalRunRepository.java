package com.saveapenny.goal.repository;

import com.saveapenny.goal.entity.GoalRunEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRunRepository extends JpaRepository<GoalRunEntity, UUID> {

    Page<GoalRunEntity> findAllByGoalIdOrderByCreatedAtDesc(UUID goalId, Pageable pageable);

    Optional<GoalRunEntity> findTopByGoalIdOrderByCreatedAtDesc(UUID goalId);
}
