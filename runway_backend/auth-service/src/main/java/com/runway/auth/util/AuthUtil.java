package com.runway.auth.util;

import com.runway.auth.model.User;
import com.runway.auth.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

	@Value("${jwt.secretKey}")
	private String secretKey;

	private SecretKey getSecretKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(User user) {
		try {
			String jwt = Jwts
				.builder()
				.setSubject(user.getUsername())
				.claim("userId", user.getUser_id())
				.claim("type", "access")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
				.signWith(SignatureAlgorithm.HS256, secretKey)
				.compact();
			System.out.println(jwt);
			return jwt;
		} catch (Exception e) {
			System.out.println("Error occured: " + e.getMessage());
		}
		return null;
	}

	public String generateRefreshToken(User user) {
		try {
			return Jwts
				.builder()
				.setSubject(user.getUsername())
				.claim("userId", user.getUser_id())
				.claim("type", "refresh")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7))
				.signWith(SignatureAlgorithm.HS256, secretKey)
				.compact();
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
		return null;
	}

	public String getUsernameFromToken(String token) {
		Claims c = Jwts.parser().setSigningKey(secretKey).build().parseSignedClaims(token).getPayload();

		return c.getSubject();
	}

	public long getUserId(String token) {
		Claims c = Jwts.parser().setSigningKey(secretKey).build().parseSignedClaims(token).getBody();
		return c.get("userId", Long.class);
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser().setSigningKey(secretKey).build().parse(token);
			return true;
		} catch (Exception e) {
			System.out.println("Exception occurred while validating token: " + e.getMessage());
			return false;
		}
	}

	public boolean validateToken(String token, String expectedType) {
		try {
			Claims claims = Jwts.parser().setSigningKey(secretKey).build().parseSignedClaims(token).getPayload();
			String tokenType = claims.get("type", String.class);
			if (!expectedType.equals(tokenType)) {
				System.out.println("Token type mismatch: expected '" + expectedType + "', got '" + tokenType + "'");
				return false;
			}
			return true;
		} catch (Exception e) {
			System.out.println("Exception occurred while validating token: " + e.getMessage());
			return false;
		}
	}

	public static Role getHighestRole(Set<Role> roles) {
		return roles.stream().max((r1, r2) -> Integer.compare(r1.getRank(), r2.getRank())).orElse(null);
	}
}

