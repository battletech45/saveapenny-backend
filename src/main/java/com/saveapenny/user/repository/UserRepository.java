package com.saveapenny.user.repository;

import com.saveapenny.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u.id from User u where u.active = true")
    List<UUID> findAllUserIds();

    @Query(value = "select u.id from User u where u.active = true",
            countQuery = "select count(u) from User u where u.active = true")
    Page<UUID> findAllUserIds(Pageable pageable);
}
