package com.runway.auth.serviceImpl;

import com.runway.auth.dao.CustomerDAO;
import com.runway.auth.dao.SellerDao;
import com.runway.auth.dao.SupportDao;
import com.runway.auth.dao.UserDao;
import com.runway.auth.dto.*;
import com.runway.auth.exceptions.UserNotFoundException;
import com.runway.auth.exceptions.UserNotFoundSellerException;
import com.runway.auth.exceptions.UserNotFoundSupportException;
import com.runway.auth.model.Customer;
import com.runway.auth.model.Seller;
import com.runway.auth.model.Support;
import com.runway.auth.model.User;
import com.runway.auth.service.AdminService;
import com.runway.auth.type.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

	private final UserDao userDao;
	private final SellerDao sellerDao;
	private final SupportDao supportDao;
	private final CustomerDAO customerDAO;

	@Override
	@Transactional
	public OnBoardSellerResponseDTO onboardSeller(OnBoardRequest requestDTO) {
		User user = null;
		String username = requestDTO.getUsername();
		System.out.println("Username received: " + username);
		try {
			user = userDao.getUserByUsername(username);
			if (user == null) {
				throw new UserNotFoundSellerException("User not found");
			}
			boolean userExists = sellerDao.checkIfSellerExists(username);
			System.out.println(userExists);
			if (userExists) {
				System.out.println("User already exists");
				return null;
			}
			Seller seller = new Seller(
				0,
				user.getName(),
				user.getUsername(),
				user.getPassword(),
				user.getEmail(),
				user.getGender(),
				user.getCreatedAt(),
				user
			);
			user.getRoles().clear();
			user.getRoles().add(Role.SELLER);
			Seller savedSeller = sellerDao.saveSeller(seller);
			if (savedSeller != null) {
				return new OnBoardSellerResponseDTO(true, savedSeller);
			} else {
				return null;
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
		return null;
	}

	@Override
	@Transactional
	public OnBoardSupportResponseDTO onboardSupport(OnBoardRequest requestDTO) {
		User user = null;
		String username = requestDTO.getUsername();
		System.out.println("Username received: " + username);
		try {
			user = userDao.getUserByUsername(username);
			if (user == null) {
				throw new UserNotFoundSupportException("User not found");
			}
			boolean userExists = supportDao.checkIfSupportExists(username);
			if (userExists) {
				System.out.println("User already exists");
				return null;
			}
			Support support = new Support(
				0,
				user.getName(),
				user.getUsername(),
				user.getPassword(),
				user.getEmail(),
				user.getGender(),
				user.getCreatedAt(),
				user
			);
			user.getRoles().clear();
			user.getRoles().add(Role.SUPPORT);
			Support savedSupport = supportDao.saveSupport(support);
			if (savedSupport != null) {
				return new OnBoardSupportResponseDTO(true, savedSupport);
			} else {
				return null;
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
		return null;
	}

	@Override
	@Transactional
	public OffboardResponseDTO offboardToCustomer(OnBoardRequest requestDTO) {
		String username = requestDTO.getUsername();
		System.out.println("Offboard request for username: " + username);

		User user = userDao.getUserByUsername(username);
		if (user == null) {
			throw new UserNotFoundException("User not found: " + username);
		}

		// Check if user is already just a Customer
		if (user.getRoles().size() == 1 && user.getRoles().contains(Role.CUSTOMER)) {
			return new OffboardResponseDTO(false, "User is already a CUSTOMER");
		}

		// Remove from Seller table if exists
		if (sellerDao.checkIfSellerExists(username)) {
			sellerDao.deleteByUserId(user.getUser_id());
			System.out.println("Removed from Seller table");
		}

		// Remove from Support table if exists
		if (supportDao.checkIfSupportExists(username)) {
			supportDao.deleteByUserId(user.getUser_id());
			System.out.println("Removed from Support table");
		}

		// Set role to CUSTOMER
		user.getRoles().clear();
		user.getRoles().add(Role.CUSTOMER);
		userDao.saveUser(user);

		// Create Customer record if not present
		if (!customerDAO.checkIfCustomerExists(username)) {
			Customer customer = Customer
				.builder()
				.name(user.getName())
				.username(user.getUsername())
				.password(user.getPassword())
				.email(user.getEmail())
				.gender(user.getGender())
				.user(user)
				.build();
			customerDAO.saveCustomer(customer);
			System.out.println("Customer record created");
		}

		return new OffboardResponseDTO(true, "User '" + username + "' offboarded to CUSTOMER successfully");
	}

	@Override
	@Transactional
	public DeleteUserResponseDTO deleteUserById(long userId) {
		User user = userDao.getUserById(userId);
		if (user == null) {
			throw new UserNotFoundException("User not found with id: " + userId);
		}

		// Always try to remove from ALL role-specific tables (order matters for FK)
		sellerDao.deleteByUserId(userId);
		supportDao.deleteByUserId(userId);
		customerDAO.deleteByUserId(userId);

		// Delete from User table
		userDao.deleteUser(user);

		return new DeleteUserResponseDTO(true, "User with id " + userId + " deleted successfully");
	}
}

