import { UserRole } from '../../core/auth.model';

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
}

export interface UserCreateRequest {
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
}

export interface UserUpdateRequest {
  fullName: string;
  email: string;
  role: UserRole;
}
