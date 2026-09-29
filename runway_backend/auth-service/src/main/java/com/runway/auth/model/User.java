package com.runway.auth.model;

import com.runway.auth.type.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(
        name="user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"username","email"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long user_id;

    @Column(nullable = false)
    private String name;
    @Column
    private String username;
    @Column
    private String email;
    @Column
    private String password;
    @Column
    private String gender;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    Set<Role> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return roles.stream()
                .map(role-> new SimpleGrantedAuthority("ROLE_"+role.name()))
                .collect(Collectors.toSet());
    }
}

