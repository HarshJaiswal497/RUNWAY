package com.runway.auth.util;

import com.runway.auth.dao.UserDao;
import com.runway.auth.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@RequiredArgsConstructor
@Slf4j
@Component
public class AuthFilter extends OncePerRequestFilter {

	private final UserDao userDao;
	private final HandlerExceptionResolver handlerExceptionResolver;
	private final AuthUtil authUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		try {
			String requestPath = request.getRequestURI();
			if (
				requestPath.startsWith("/swagger-ui") ||
				requestPath.startsWith("/v3/api-docs") ||
				requestPath.startsWith("/webjars/") ||
				requestPath.startsWith("/actuator/")
			) {
				filterChain.doFilter(request, response);
				return;
			}

			final String tokenHeader = request.getHeader("Authorization");
			if (tokenHeader == null) {
				filterChain.doFilter(request, response);
				return;
			}
			String token = tokenHeader.substring(7);
			if (!authUtil.validateToken(token)) {
				System.out.println("Token not valid, skipping...");
				filterChain.doFilter(request, response);
				return;
			}
			String username = authUtil.getUsernameFromToken(token);
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				User user = userDao.getUserByUsername(username);
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
					user,
					null,
					user.getAuthorities()
				);
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}
		} catch (Exception e) {
			handlerExceptionResolver.resolveException(request, response, null, e);
		}
		filterChain.doFilter(request, response);
	}
}

