package com.runway.auth.service;

import com.runway.auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO, HttpServletResponse httpServletResponse);
    public LoginResponseDTO refresh(HttpServletRequest request);
    public SignUpResponseDTO signup(SignUpRequestDTO signUpRequestDTO) throws Exception;
    public ValidateResponseDTO validateToken(String token);
    public UserDetailsResponseDTO getUserDetails(long userId);
    public DeleteUserResponseDTO deleteAccount(DeleteAccountRequestDTO requestDTO);
}

