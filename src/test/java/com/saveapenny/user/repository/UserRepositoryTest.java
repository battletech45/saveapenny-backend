package com.saveapenny.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.user.entity.User;
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
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User entity;

    @BeforeEach
    void setUp() {
        entity = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("hashed-password")
                .fullName("Alice")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        userRepository.save(entity);
    }

    @Test
    void findByEmail_returnsUserWhenExists() {
        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertTrue(found.isPresent());
        assertEquals(entity.getId(), found.get().getId());
    }

    @Test
    void findByEmail_returnsEmptyWhenNotExists() {
        Optional<User> found = userRepository.findByEmail("unknown@example.com");
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmail_returnsTrueWhenExists() {
        assertTrue(userRepository.existsByEmail("alice@example.com"));
    }

    @Test
    void existsByEmail_returnsFalseWhenNotExists() {
        assertFalse(userRepository.existsByEmail("unknown@example.com"));
    }

    @Test
    void save_persistsNewUser() {
        User newUser = User.builder()
                .id(UUID.randomUUID())
                .email("bob@example.com")
                .passwordHash("hash")
                .fullName("Bob")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        User saved = userRepository.save(newUser);

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("bob@example.com", found.get().getEmail());
    }
}
