package com.runway.auth.service;

import com.runway.auth.dto.*;

public interface AdminService {
    public OnBoardSellerResponseDTO onboardSeller(OnBoardRequest requestDTO);
    public OnBoardSupportResponseDTO onboardSupport(OnBoardRequest requestDTO);
    public OffboardResponseDTO offboardToCustomer(OnBoardRequest requestDTO);
    public DeleteUserResponseDTO deleteUserById(long userId);
}

