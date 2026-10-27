import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductRequest, SupplierProductRequest } from './product';
import { API_BASE } from '../../core/api-base';

@Injectable({
  providedIn: 'root'
})
export class ProductApi {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE}/api/products`;

  getAll(): Observable<Product[]> {
    return this.http.get<Product[]>(this.apiUrl);
  }

  
  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  
  addSupplier(
    productId: number,
    request: SupplierProductRequest
  ): Observable<Product> {
    return this.http.post<Product>(
      `${this.apiUrl}/${productId}/suppliers`,
      request
    );
  }

  updateSupplier(
    productId: number,
    supplierId: number,
    request: SupplierProductRequest
  ): Observable<Product> {
    return this.http.put<Product>(
      `${this.apiUrl}/${productId}/suppliers/${supplierId}`,
      request
    );
  }

  removeSupplier(
    productId: number,
    supplierId: number
  ): Observable<Product> {
    return this.http.delete<Product>(
      `${this.apiUrl}/${productId}/suppliers/${supplierId}`
    );
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, request);
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(
      `${this.apiUrl}/${id}`,
      request
    );
  }

  reactivate(id: number): Observable<Product> {
    return this.http.patch<Product>(
      `${this.apiUrl}/${id}/reactivate`,
      {}
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}