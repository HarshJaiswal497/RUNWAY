export interface LoginRequest {
  username: string;
  password: string;
}

export interface SignupRequest {
  username: string;
  password: string;
  name: string;
  gender: string;
  email: string;
}

export interface LoginResponse {
  success: boolean;
  userId: number;
  token: string;
}

export interface LoggedInUser {
  userId: number;
  username: string;
  token: string;
  role: string;
}