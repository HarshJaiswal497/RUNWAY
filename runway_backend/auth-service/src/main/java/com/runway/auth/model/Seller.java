package com.runway.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Seller {

    @Id
    private long user_id;

    @Column(nullable = false)
    private String name;

    @Column
    private String username;

    @Column
    private String password;

    @Column
    private String email;

    @Column
    private String gender;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToOne
    @MapsId
    private User user;

}

