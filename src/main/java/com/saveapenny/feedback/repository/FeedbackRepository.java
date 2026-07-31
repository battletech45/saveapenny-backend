package com.saveapenny.feedback.repository;

import com.saveapenny.feedback.entity.Feedback;
import com.saveapenny.feedback.entity.FeedbackType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    Optional<Feedback> findByIdAndUserId(UUID id, UUID userId);

    Page<Feedback> findAllByUserId(UUID userId, Pageable pageable);

    Page<Feedback> findAllByUserIdAndType(UUID userId, FeedbackType type, Pageable pageable);
}
