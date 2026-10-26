
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  PurchaseOrder,
  PurchaseOrderCreateRequest,
  PurchaseOrderUpdateRequest,
  PurchaseOrderReceiveRequest
} from './purchase-order';

@Injectable({
  providedIn: 'root'
})
export class PurchaseOrderApi {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/purchase-orders';

  getAll(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(this.apiUrl);
  }

  getById(id: number): Observable<PurchaseOrder> {
    return this.http.get<PurchaseOrder>(`${this.apiUrl}/${id}`);
  }

  create(request: PurchaseOrderCreateRequest): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(this.apiUrl, request);
  }

  update(
    id: number,
    request: PurchaseOrderUpdateRequest
  ): Observable<PurchaseOrder> {
    return this.http.put<PurchaseOrder>(`${this.apiUrl}/${id}`, request);
  }

  order(id: number): Observable<PurchaseOrder> {
    return this.http.patch<PurchaseOrder>(
      `${this.apiUrl}/${id}/order`,
      {}
    );
  }

  receive(
    id: number,
    request: PurchaseOrderReceiveRequest
  ): Observable<PurchaseOrder> {
    return this.http.patch<PurchaseOrder>(
      `${this.apiUrl}/${id}/receive`,
      request
    );
  }

  archive(id: number): Observable<PurchaseOrder> {
    return this.http.patch<PurchaseOrder>(`${this.apiUrl}/${id}/archive`, {});
  }

  cancel(id: number): Observable<PurchaseOrder> {
    return this.http.patch<PurchaseOrder>(
      `${this.apiUrl}/${id}/cancel`,
      {}
    );
  }
}
