package com.runway.auth.repository;

import com.runway.auth.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller,Long> {
    Optional<Seller> findByUsername(String username);
}

