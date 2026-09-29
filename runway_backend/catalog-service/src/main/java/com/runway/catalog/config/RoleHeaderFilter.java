package com.runway.catalog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RoleHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        String roleHeader = request.getHeader("X-USER-ROLE");
        if (roleHeader == null) roleHeader = request.getHeader("X-User-Role");

        if (roleHeader != null) {
            // If it's "ADMIN,CUSTOMER", we want to pick the most powerful one for the SecurityContext
            String bestRole = "CUSTOMER"; // default
            if (roleHeader.toUpperCase().contains("ADMIN")) {
                bestRole = "ADMIN";
            } else if (roleHeader.toUpperCase().contains("SELLER")) {
                bestRole = "SELLER";
            }

            String formattedRole = "ROLE_" + bestRole;
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(formattedRole);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    request.getHeader("X-USER-ID"), null, Collections.singletonList(authority));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}