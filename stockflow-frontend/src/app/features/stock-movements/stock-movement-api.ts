
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  StockMovement,
  StockMovementRequest
} from './stock-movement';

@Injectable({
  providedIn: 'root'
})
export class StockMovementApi {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/stock-movements';

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
