import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardData } from './dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardApi {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/dashboard';

  get(): Observable<DashboardData> {
    return this.http.get<DashboardData>(this.apiUrl);
  }
}
