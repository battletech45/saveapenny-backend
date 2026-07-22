package com.saveapenny.goal.repository;

import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {

    Page<GoalEntity> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Page<GoalEntity> findAllByUserIdAndStatusAndDeletedAtIsNull(UUID userId, GoalStatus status, Pageable pageable);

    Page<GoalEntity> findAllByUserIdAndTypeAndDeletedAtIsNull(UUID userId, GoalType type, Pageable pageable);

    Page<GoalEntity> findAllByUserIdAndStatusAndTypeAndDeletedAtIsNull(
            UUID userId,
            GoalStatus status,
            GoalType type,
            Pageable pageable);

    Page<GoalEntity> findAllByStatusAndDeletedAtIsNull(GoalStatus status, Pageable pageable);

    Optional<GoalEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    long countByUserIdAndStatusAndDeletedAtIsNull(UUID userId, GoalStatus status);
}
