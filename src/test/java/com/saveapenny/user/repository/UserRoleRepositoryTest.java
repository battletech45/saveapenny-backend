package com.saveapenny.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.user.entity.Role;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.entity.UserRole;
import com.saveapenny.user.entity.UserRoleId;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class UserRoleRepositoryTest {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hash")
                .fullName("Test User")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(user);

        Role role = Role.builder()
                .id(roleId)
                .name("ROLE_TEST")
                .build();
        entityManager.persist(role);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(userId, roleId))
                .user(user)
                .role(role)
                .build();
        entityManager.persist(userRole);

        entityManager.flush();
    }

    @Test
    void findAllByIdUserId_returnsUserRoles() {
        List<UserRole> userRoles = userRoleRepository.findAllByIdUserId(userId);
        assertEquals(1, userRoles.size());
        assertEquals(roleId, userRoles.getFirst().getRole().getId());
    }

    @Test
    void findAllByIdUserId_returnsEmpty_whenNoRoles() {
        List<UserRole> userRoles = userRoleRepository.findAllByIdUserId(UUID.randomUUID());
        assertEquals(0, userRoles.size());
    }
}
