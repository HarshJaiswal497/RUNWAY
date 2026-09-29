package com.runway.auth.exceptions;

import com.runway.auth.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<LoginResponseDTO> handleBadCredentials() {
		LoginResponseDTO result = new LoginResponseDTO(false, 0L, null);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
	}

	@ExceptionHandler({ DisabledException.class, LockedException.class })
	public ResponseEntity<LoginResponseDTO> handleAccountNotAccessible() {
		LoginResponseDTO result = new LoginResponseDTO(false, 0L, null);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
	}

	@ExceptionHandler(InvalidFieldException.class)
	public ResponseEntity<LoginResponseDTO> handleInvalidField(InvalidFieldException ex) {
		LoginResponseDTO result = new LoginResponseDTO(false, 0L, null);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ValidateResponseDTO> handleInvalidToken() {
		ValidateResponseDTO result = new ValidateResponseDTO(false, 0L, null);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
	}

	@ExceptionHandler(UserNotSavedException.class)
	public ResponseEntity<SignUpResponseDTO> handleUserNotSaved() {
		SignUpResponseDTO result = new SignUpResponseDTO(false, null, 0L);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
	}

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<SignUpResponseDTO> handleUserAlreadyExists() {
		SignUpResponseDTO result = new SignUpResponseDTO(false, null, 0L);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<UserDetailsResponseDTO> handleUserDoesNotExist() {
		UserDetailsResponseDTO details = new UserDetailsResponseDTO(false, null);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(details);
	}

	@ExceptionHandler(UserNotFoundSupportException.class)
	public ResponseEntity<OnBoardSupportResponseDTO> handleUserDoesNotExistForSupport() {
		OnBoardSupportResponseDTO details = new OnBoardSupportResponseDTO(false, null);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(details);
	}

	@ExceptionHandler(UserNotFoundSellerException.class)
	public ResponseEntity<OnBoardSellerResponseDTO> handleUserDoesNotExistForSeller() {
		OnBoardSellerResponseDTO details = new OnBoardSellerResponseDTO(false, null);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(details);
	}
}

