package com.runway.auth.service;

import com.runway.auth.dao.SellerDao;
import com.runway.auth.dao.SupportDao;
import com.runway.auth.dao.UserDao;
import com.runway.auth.dto.OnBoardRequest;
import com.runway.auth.dto.OnBoardSellerResponseDTO;
import com.runway.auth.dto.OnBoardSupportResponseDTO;
import com.runway.auth.model.Seller;
import com.runway.auth.model.Support;
import com.runway.auth.model.User;
import com.runway.auth.serviceImpl.AdminServiceImpl;
import com.runway.auth.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

	@Mock private UserDao userDao;
	@Mock private SellerDao sellerDao;
	@Mock private SupportDao supportDao;

	@InjectMocks
	private AdminServiceImpl adminServiceImpl;

	private User testUser;
	private OnBoardRequest request;

	@BeforeEach
	void setUp() {
		Set<Role> roles = new HashSet<>();
		roles.add(Role.CUSTOMER);
		testUser = User.builder()
				.user_id(1L).name("John Doe").username("johndoe")
				.email("john@example.com").password("encoded").gender("Male")
				.createdAt(LocalDateTime.now()).roles(roles).build();
		request = new OnBoardRequest("johndoe");
	}

	@Test
	@DisplayName("onboardSeller â€“ success")
	void onboardSeller_Success() {
		when(userDao.getUserByUsername("johndoe")).thenReturn(testUser);
		when(sellerDao.checkIfSellerExists("johndoe")).thenReturn(false);
		when(sellerDao.saveSeller(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));

		OnBoardSellerResponseDTO result = adminServiceImpl.onboardSeller(request);

		assertNotNull(result);
		assertTrue(result.isSuccess());
		assertTrue(testUser.getRoles().contains(Role.SELLER));
	}

	@Test
	@DisplayName("onboardSeller â€“ user not found returns null")
	void onboardSeller_UserNotFound() {
		when(userDao.getUserByUsername("johndoe")).thenReturn(null);

		assertNull(adminServiceImpl.onboardSeller(request));
	}

	@Test
	@DisplayName("onboardSupport â€“ success")
	void onboardSupport_Success() {
		when(userDao.getUserByUsername("johndoe")).thenReturn(testUser);
		when(supportDao.checkIfSupportExists("johndoe")).thenReturn(false);
		when(supportDao.saveSupport(any(Support.class))).thenAnswer(inv -> inv.getArgument(0));

		OnBoardSupportResponseDTO result = adminServiceImpl.onboardSupport(request);

		assertNotNull(result);
		assertTrue(result.isSuccess());
		assertTrue(testUser.getRoles().contains(Role.SUPPORT));
	}

	@Test
	@DisplayName("onboardSupport â€“ user not found returns null")
	void onboardSupport_UserNotFound() {
		when(userDao.getUserByUsername("johndoe")).thenReturn(null);

		assertNull(adminServiceImpl.onboardSupport(request));
	}
}

