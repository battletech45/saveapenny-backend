package com.saveapenny.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.user.entity.Role;
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
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_TEST")
                .build();
        roleRepository.save(role);
    }

    @Test
    void findByName_returnsRole() {
        Optional<Role> found = roleRepository.findByName("ROLE_TEST");
        assertTrue(found.isPresent());
        assertEquals(role.getId(), found.get().getId());
    }

    @Test
    void findByName_returnsEmpty_whenNotFound() {
        assertTrue(roleRepository.findByName("ROLE_NONEXISTENT").isEmpty());
    }
}
