import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserCreateRequest, UserUpdateRequest } from './user';

@Injectable({
  providedIn: 'root'
})
export class UserApi {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/users';

  getAll(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  create(request: UserCreateRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl, request);
  }

  update(id: number, request: UserUpdateRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, request);
  }

  updatePassword(id: number, password: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/password`, { password });
  }

  disable(id: number): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/disable`, {});
  }

  enable(id: number): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/enable`, {});
  }
}
