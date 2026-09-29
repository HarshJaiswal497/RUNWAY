package com.runway.auth.serviceImpl;

import com.runway.auth.client.EngagementServiceClient;
import com.runway.auth.dao.CustomerDAO;
import com.runway.auth.dao.SellerDao;
import com.runway.auth.dao.SupportDao;
import com.runway.auth.dao.UserDao;
import com.runway.auth.dto.*;
import com.runway.auth.exceptions.*;
import com.runway.auth.model.Customer;
import com.runway.auth.model.User;
import com.runway.auth.repository.UserRepository;
import com.runway.auth.service.AuthService;
import com.runway.auth.type.Role;
import com.runway.auth.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final AuthUtil authUtil;
	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;
	private final UserDetailsServiceImpl userDetailsService;
	private final CustomerDAO customerDAO;
	private final SellerDao sellerDao;
	private final SupportDao supportDao;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private EngagementServiceClient engagementClient;

    @Override
    @Transactional
	public LoginResponseDTO login(LoginRequestDTO loginRequestDTO, HttpServletResponse httpServletResponse) {
		if (loginRequestDTO.getUsername() == null || loginRequestDTO.getUsername().isBlank()) {
			throw new InvalidFieldException("Username must not be empty");
		}
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(), loginRequestDTO.getPassword())
		);
		User user = (User) authentication.getPrincipal();
		String accessToken = authUtil.generateAccessToken(user);
		if (accessToken != null) {
			System.out.println("Access token generated successfully.");
		}
		String refreshToken = authUtil.generateRefreshToken(user);
		ResponseCookie responseCookie = ResponseCookie
			.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(false)
			.path("/auth/refreshtoken")
			.maxAge(60 * 60 * 24 * 7)
			.build();
		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
		return new LoginResponseDTO(true, user.getUser_id(), accessToken);
	}

    @Override
	public LoginResponseDTO refresh(HttpServletRequest request) {
		// Guard against null cookies (no cookies sent at all)
		if (request.getCookies() == null) {
			throw new InvalidTokenException("No refresh token cookie found");
		}

		String refreshToken = Arrays
			.stream(request.getCookies())
			.filter(cookie -> cookie.getName().equalsIgnoreCase("refreshtoken"))
			.findFirst()
			.map(cookie -> cookie.getValue())
			.orElse(null);

		if (refreshToken == null) {
			throw new InvalidTokenException("Refresh token cookie is missing");
		}

		// Validate that this is actually a REFRESH token, not an access token
		if (!authUtil.validateToken(refreshToken, "refresh")) {
			throw new InvalidTokenException("Invalid or expired refresh token");
		}

		String username = authUtil.getUsernameFromToken(refreshToken);
		User user;
		try {
			user = (User) userDetailsService.loadUserByUsername(username);
		} catch (UsernameNotFoundException e) {
			System.out.println("User not found: " + e.getMessage());
			throw new InvalidTokenException("User associated with refresh token not found");
		}

		long userId = user.getUser_id();
		String accessToken = authUtil.generateAccessToken(user);
		return new LoginResponseDTO(true, userId, accessToken);
	}

    @Override
    @Transactional
	public SignUpResponseDTO signup(SignUpRequestDTO signUpRequestDTO) throws Exception {
		if (signUpRequestDTO.getUsername() == null || signUpRequestDTO.getUsername().isBlank()) {
			throw new InvalidFieldException("Username must not be empty");
		}
		if (signUpRequestDTO.getName() == null || signUpRequestDTO.getName().isBlank()) {
			throw new InvalidFieldException("Name must not be empty");
		}
		if (signUpRequestDTO.getGender() == null || signUpRequestDTO.getGender().isBlank()) {
			throw new InvalidFieldException("Gender must not be empty");
		}
		if (signUpRequestDTO.getPassword().length() < 8 || signUpRequestDTO.getPassword().isBlank()) {
			throw new InvalidFieldException("Password must not be empty or lesser than 8 characters");
		}
		if (signUpRequestDTO.getEmail() == null || signUpRequestDTO.getEmail().isBlank()) {
			throw new InvalidFieldException("Email must not be empty");
		}
		User user = userDao.getUserByUsername(signUpRequestDTO.getUsername());
		User userSaved = null;
		if (user != null) {
			throw new UserAlreadyExistsException("User already exists");
		} else {
			User newUser = User
				.builder()
				.name(signUpRequestDTO.getName())
				.username(signUpRequestDTO.getUsername())
				.roles(Set.of(Role.CUSTOMER))
				.gender(signUpRequestDTO.getGender())
				.email(signUpRequestDTO.getEmail())
				.password(passwordEncoder.encode(signUpRequestDTO.getPassword()))
				.build();
			userSaved = userDao.saveUser(newUser);
			if (userSaved == null) {
				throw new UserNotSavedException("Unable to insert into User DB");
			} else {
				System.out.println("User table updated successfully.");
			}
			Customer customer = Customer
				.builder()
				.name(userSaved.getName())
				.username(userSaved.getUsername())
				.gender(userSaved.getGender())
				.email(userSaved.getEmail())
				.password(userSaved.getPassword())
				.user(userSaved)
				.build();
			boolean customerSaved = customerDAO.saveCustomer(customer);
			if (!customerSaved) {
				throw new UserNotSavedException("Unable to insert into Customer DB");
			} else {
				System.out.println("Customer table updated successfully.");
			}
			notifyEngagement("ACCOUNT_CREATED", userSaved.getUser_id(), Map.of(
				"userName", userSaved.getName(),
				"email", userSaved.getEmail()
			));
			return new SignUpResponseDTO(true, userSaved.getUsername(), userSaved.getUser_id());
		}
	}

    @Override
	public ValidateResponseDTO validateToken(String token) {
		boolean checkIfValid = authUtil.validateToken(token);
		if (!checkIfValid) {
			throw new InvalidTokenException("Token not valid");
		}
		long userId = authUtil.getUserId(token);
		User user = userDao.getUserById(userId);
		Role highestRole = authUtil.getHighestRole(user.getRoles());
		return new ValidateResponseDTO(checkIfValid, userId, highestRole.name());
	}

    public List<UserDetailsResponseDTO> getAllUsers(String filterRole) {
        List<User> users;
        if (filterRole != null && !filterRole.isBlank()) {
            try {
                Role roleEnum = Role.valueOf(filterRole.toUpperCase());
                users = userRepository.findByRole(roleEnum);
            } catch (IllegalArgumentException e) {
                users = userRepository.findAll();
            }
        } else {
            users = userRepository.findAll();
        }
        return users.stream()
                .map(user -> new UserDetailsResponseDTO(true, user))
                .toList();
    }

    @Override
	public UserDetailsResponseDTO getUserDetails(long userId) {
		User user = userDao.getUserById(userId);
		if (user == null) {
			throw new UserNotFoundException("User does not exist");
		}
		return new UserDetailsResponseDTO(true, user);
	}


	@Override
	@Transactional
	public DeleteUserResponseDTO deleteAccount(DeleteAccountRequestDTO requestDTO) {
		if (requestDTO.getUsername() == null || requestDTO.getUsername().isBlank()) {
			throw new InvalidFieldException("Username must not be empty");
		}
		if (requestDTO.getPassword() == null || requestDTO.getPassword().isBlank()) {
			throw new InvalidFieldException("Password must not be empty");
		}

		// Authenticate the user with provided credentials
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(requestDTO.getUsername(), requestDTO.getPassword())
		);

		// Credentials are valid â€” proceed with deletion
		User user = userDao.getUserByUsername(requestDTO.getUsername());
		if (user == null) {
			throw new UserNotFoundException("User not found: " + requestDTO.getUsername());
		}

		long userId = user.getUser_id();

		// Always try to remove from ALL role-specific tables (order matters for FK)
		sellerDao.deleteByUserId(userId);
		supportDao.deleteByUserId(userId);
		customerDAO.deleteByUserId(userId);

		// Delete from User table
		userDao.deleteUser(user);

		return new DeleteUserResponseDTO(true, "Account '" + requestDTO.getUsername() + "' deleted successfully");
	}

	private void notifyEngagement(String event, Long userId, Map<String, Object> payload) {
		if (engagementClient == null) return;
		try {
			engagementClient.notify(new EngagementServiceClient.NotifyRequest(event, userId, payload));
		} catch (Exception e) {
			System.out.println("Notification skipped (engagement service unavailable): event=" + event);
		}
	}

}

