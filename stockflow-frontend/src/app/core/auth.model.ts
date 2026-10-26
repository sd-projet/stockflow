export type UserRole = 'ADMIN' | 'STOCK_MANAGER' | 'MANAGER';

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  fullName: string;
  email: string;
  role: UserRole;
}

export interface StoredUser {
  userId: number;
  fullName: string;
  email: string;
  role: UserRole;
}
