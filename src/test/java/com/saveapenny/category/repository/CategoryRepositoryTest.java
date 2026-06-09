package com.saveapenny.category.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private UUID userId;
    private Category userCategory;
    private Category systemCategory;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        systemCategory = Category.builder()
                .id(UUID.randomUUID())
                .userId(null)
                .name("Salary")
                .type(CategoryType.INCOME)
                .color("#00aa00")
                .icon("briefcase")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        categoryRepository.save(systemCategory);

        userCategory = Category.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .color("#ff0000")
                .icon("utensils")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        categoryRepository.save(userCategory);
    }

    @Test
    void findAllByUserIdIsNullAndType_returnsSystemCategories() {
        var results = categoryRepository.findAllByUserIdIsNullAndType(CategoryType.INCOME);
        assertEquals(1, results.size());
        assertEquals("Salary", results.getFirst().getName());
    }

    @Test
    void findAllByUserIdAndType_returnsUserCategories() {
        var results = categoryRepository.findAllByUserIdAndType(userId, CategoryType.EXPENSE);
        assertEquals(1, results.size());
        assertEquals("Food", results.getFirst().getName());
    }

    @Test
    void findAllByUserIdIsNullOrUserIdAndType_returnsBoth() {
        var results = categoryRepository.findAllByUserIdIsNullOrUserIdAndType(userId, CategoryType.INCOME);
        assertTrue(results.size() >= 1);
    }

    @Test
    void findByIdAndUserId_returnsUserCategory() {
        Optional<Category> found = categoryRepository.findByIdAndUserId(userCategory.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals("Food", found.get().getName());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        Optional<Category> found = categoryRepository.findByIdAndUserId(userCategory.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdAndUserIdIsNull_returnsSystemCategory() {
        Optional<Category> found = categoryRepository.findByIdAndUserIdIsNull(systemCategory.getId());
        assertTrue(found.isPresent());
    }

    @Test
    void existsByUserIdAndNameIgnoreCaseAndType_checksDuplicate() {
        assertTrue(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "food", CategoryType.EXPENSE));
        assertFalse(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "food", CategoryType.INCOME));
    }

    @Test
    void existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot_excludesOwnId() {
        boolean exists = categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
                userId, "food", CategoryType.EXPENSE, userCategory.getId());
        assertFalse(exists);
    }
}
