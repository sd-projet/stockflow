import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  StockMovement,
  StockMovementRequest
} from './stock-movement';
import { API_BASE } from '../../core/api-base';

@Injectable({
  providedIn: 'root'
})
export class StockMovementApi {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE}/api/stock-movements`;

  getAll(): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(this.apiUrl);
  }

  getByProductId(productId: number): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(
      `${this.apiUrl}/product/${productId}`
    );
  }

  create(request: StockMovementRequest): Observable<StockMovement> {
    return this.http.post<StockMovement>(this.apiUrl, request);
  }
}