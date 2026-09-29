package com.runway.auth.repository;

import com.runway.auth.model.Support;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportRepository  extends JpaRepository<Support,Long> {
    Optional<Support> findByUsername(String username);
}
