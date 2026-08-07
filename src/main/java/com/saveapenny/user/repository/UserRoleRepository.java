package com.saveapenny.user.repository;

import com.saveapenny.user.entity.UserRole;
import com.saveapenny.user.entity.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByIdUserId(UUID userId);

    @Query("select ur.role.name from UserRole ur where ur.id.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}
