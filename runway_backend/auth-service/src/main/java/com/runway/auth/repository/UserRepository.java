package com.runway.auth.repository;

import com.runway.auth.model.User;
import com.runway.auth.type.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") Role role);
}

