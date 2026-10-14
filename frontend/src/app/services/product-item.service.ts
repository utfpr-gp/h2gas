import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { ProductItem } from '../models/product-item';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../models/paginated-response';

@Injectable({
  providedIn: 'root'
})
export class ProductItemService extends AbstractService<ProductItem> {
  override baseUrl: string = `${environment.apiUrl}/product-items`

  constructor(protected override http: HttpClient) {
    super(http);
  }

  // getProductsBySupllierId(supplierId: string): Observable<PaginatedResponse<ProductItem>> {
  //   return this.http.get<PaginatedResponse<ProductItem>>(`${this.baseUrl}/by-supplier/${supplierId}`);
  // }  

  getProductsBySupllierId(supplierId: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<ProductItem>> {
    return this.http.get<PaginatedResponse<ProductItem>>(
      `${this.baseUrl}/by-supplier/${supplierId}?page=${page}&size=${size}`
    );
  }

  getProductsBySupplierId(
    supplierId: string,
    page: number = 0,
    size: number = 10,
    search?: string,
    category?: string
  ): Observable<PaginatedResponse<ProductItem>> {
    let params = `?page=${page}&size=${size}`;
    if (search) params += `&search=${encodeURIComponent(search)}`;
    if (category) params += `&category=${encodeURIComponent(category)}`;
  
    return this.http.get<PaginatedResponse<ProductItem>>(
      `${this.baseUrl}/by-supplier/${supplierId}${params}`
    );
  }
  

  getProductsFromActiveSuppliers(
    page: number = 0,
    size: number = 20,
    cityId?: number,
    category?: string,
    sort?: string
  ): Observable<PaginatedResponse<ProductItem>> {
    let params: Record<string, string> = { page: page.toString(), size: size.toString() };
    if (cityId != null) params['cityId'] = cityId.toString();
    if (category) params['category'] = category;
    if (sort) params['sort'] = sort;

    return this.http.get<PaginatedResponse<ProductItem>>(`${this.baseUrl}/by-supplier-open`, { params });
  }

  createWithFile(product: ProductItem, file: File): Observable<ProductItem> {
    const formData = new FormData();
    
    formData.append('product', new Blob([JSON.stringify(product)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<ProductItem>(`${this.baseUrl}`, formData);
  }
}
