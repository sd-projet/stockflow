import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, StoredUser, UserRole } from './auth.model';

@Injectable({
  providedIn: 'root'
})
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/auth';

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, request)
      .pipe(tap((response) => this.storeSession(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, request)
      .pipe(tap((response) => this.storeSession(response)));
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem('stockflow_token', response.token);
    localStorage.setItem(
      'stockflow_user',
      JSON.stringify({
        userId: response.userId,
        fullName: response.fullName,
        email: response.email,
        role: response.role
      })
    );
  }

  getToken(): string | null {
    return localStorage.getItem('stockflow_token');
  }

  getUser(): StoredUser | null {
    const raw = localStorage.getItem('stockflow_user');

    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as StoredUser;
    } catch {
      return null;
    }
  }

  hasRole(...roles: UserRole[]): boolean {
    const user = this.getUser();

    return user !== null && roles.includes(user.role);
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  /**
   * Un jeton absent ou expiré est considéré comme non authentifié.
   * Le serveur reste l'autorité : cette vérification évite seulement
   * d'afficher des pages qui renverraient immédiatement une erreur 401.
   */
  isAuthenticated(): boolean {
    const token = this.getToken();

    if (!token) {
      return false;
    }

    const expiration = this.getTokenExpiration(token);

    if (expiration !== null && expiration * 1000 <= Date.now()) {
      this.logout();
      return false;
    }

    return true;
  }

  logout(): void {
    localStorage.removeItem('stockflow_token');
    localStorage.removeItem('stockflow_user');
  }

  private getTokenExpiration(token: string): number | null {
    try {
      const payload = token.split('.')[1];

      if (!payload) {
        return null;
      }

      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      const exp = JSON.parse(json)?.exp;

      return typeof exp === 'number' ? exp : null;
    } catch {
      return null;
    }
  }
}
