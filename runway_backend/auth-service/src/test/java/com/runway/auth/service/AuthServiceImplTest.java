package com.runway.auth.service;

import com.runway.auth.dao.CustomerDAO;
import com.runway.auth.dao.UserDao;
import com.runway.auth.dto.*;
import com.runway.auth.exceptions.*;
import com.runway.auth.model.Customer;
import com.runway.auth.model.User;
import com.runway.auth.serviceImpl.AuthServiceImpl;
import com.runway.auth.serviceImpl.UserDetailsServiceImpl;
import com.runway.auth.type.Role;
import com.runway.auth.util.AuthUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock private AuthenticationManager authenticationManager;
	@Mock private AuthUtil authUtil;
	@Mock private UserDao userDao;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private UserDetailsServiceImpl userDetailsService;
	@Mock private CustomerDAO customerDAO;

	@InjectMocks
	private AuthServiceImpl authServiceImpl;

	private User testUser;

	@BeforeEach
	void setUp() {
		Set<Role> roles = new HashSet<>();
		roles.add(Role.CUSTOMER);
		testUser = User.builder()
				.user_id(1L).name("John Doe").username("johndoe")
				.email("john@example.com").password("encoded").gender("Male")
				.roles(roles).build();
	}

	@Test
	@DisplayName("login â€“ success")
	void login_Success() {
		LoginRequestDTO req = new LoginRequestDTO("johndoe", "pass123");
		HttpServletResponse res = mock(HttpServletResponse.class);
		Authentication auth = mock(Authentication.class);

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
		when(auth.getPrincipal()).thenReturn(testUser);
		when(authUtil.generateAccessToken(testUser)).thenReturn("access-tok");
		when(authUtil.generateRefreshToken(testUser)).thenReturn("refresh-tok");

		LoginResponseDTO result = authServiceImpl.login(req, res);

		assertTrue(result.isSuccess());
		assertEquals(1L, result.getUserId());
		assertEquals("access-tok", result.getToken());
		verify(res).addHeader(eq("Set-Cookie"), anyString());
	}

	@Test
	@DisplayName("login â€“ blank username throws InvalidFieldException")
	void login_BlankUsername() {
		assertThrows(InvalidFieldException.class,
				() -> authServiceImpl.login(new LoginRequestDTO("  ", "pass"), mock(HttpServletResponse.class)));
	}

	@Test
	@DisplayName("refresh â€“ success with valid cookie")
	void refresh_Success() {
		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("refreshToken", "rt")});
		when(authUtil.validateToken("rt", "refresh")).thenReturn(true);
		when(authUtil.getUsernameFromToken("rt")).thenReturn("johndoe");
		when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(testUser);
		when(authUtil.generateAccessToken(testUser)).thenReturn("new-at");

		LoginResponseDTO result = authServiceImpl.refresh(req);

		assertTrue(result.isSuccess());
		assertEquals("new-at", result.getToken());
	}

	@Test
	@DisplayName("signup â€“ success")
	void signup_Success() throws Exception {
		SignUpRequestDTO req = new SignUpRequestDTO("newuser", "password1234", "New User", "Male", "new@example.com");
		User saved = User.builder().user_id(2L).username("newuser").name("New User")
				.email("new@example.com").password("enc").gender("Male").roles(Set.of(Role.CUSTOMER)).build();

		when(userDao.getUserByUsername("newuser")).thenReturn(null);
		when(passwordEncoder.encode("password1234")).thenReturn("enc");
		when(userDao.saveUser(any(User.class))).thenReturn(saved);
		when(customerDAO.saveCustomer(any(Customer.class))).thenReturn(true);

		SignUpResponseDTO result = authServiceImpl.signup(req);

		assertTrue(result.isSuccess());
		assertEquals("newuser", result.getUsername());
	}

	@Test
	@DisplayName("signup â€“ existing user throws UserAlreadyExistsException")
	void signup_UserAlreadyExists() {
		SignUpRequestDTO req = new SignUpRequestDTO("johndoe", "password1234", "John", "Male", "j@e.com");
		when(userDao.getUserByUsername("johndoe")).thenReturn(testUser);
		assertThrows(UserAlreadyExistsException.class, () -> authServiceImpl.signup(req));
	}

	@Test
	@DisplayName("validateToken â€“ valid token returns response")
	void validateToken_Success() {
		when(authUtil.validateToken("tok")).thenReturn(true);
		when(authUtil.getUserId("tok")).thenReturn(1L);
		when(userDao.getUserById(1L)).thenReturn(testUser);

		ValidateResponseDTO result = authServiceImpl.validateToken("tok");

		assertTrue(result.isValid());
		assertEquals("CUSTOMER", result.getRole());
	}

	@Test
	@DisplayName("validateToken â€“ invalid token throws InvalidTokenException")
	void validateToken_Invalid() {
		when(authUtil.validateToken("bad")).thenReturn(false);
		assertThrows(InvalidTokenException.class, () -> authServiceImpl.validateToken("bad"));
	}

	@Test
	@DisplayName("getUserDetails â€“ user not found throws UserNotFoundException")
	void getUserDetails_NotFound() {
		when(userDao.getUserById(99L)).thenReturn(null);
		assertThrows(UserNotFoundException.class, () -> authServiceImpl.getUserDetails(99L));
	}
}

