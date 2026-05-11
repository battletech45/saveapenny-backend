package com.saveapenny.user.repository;

import com.saveapenny.user.entity.UserRole;
import com.saveapenny.user.entity.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByIdUserId(UUID userId);
}
