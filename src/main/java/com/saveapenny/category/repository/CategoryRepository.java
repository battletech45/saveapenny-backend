package com.saveapenny.category.repository;

import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByUserIdIsNullAndType(CategoryType type);

    List<Category> findAllByUserIdAndType(UUID userId, CategoryType type);

    List<Category> findAllByUserIdIsNullOrUserIdAndType(UUID userId, CategoryType type);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    Optional<Category> findByIdAndUserIdIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Category c where c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByUserIdAndNameIgnoreCaseAndType(UUID userId, String name, CategoryType type);

    boolean existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(UUID userId, String name, CategoryType type, UUID id);
}
