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

  getProductsBySupllierId(supplierId: string): Observable<PaginatedResponse<ProductItem>> {
    return this.http.get<PaginatedResponse<ProductItem>>(`${this.baseUrl}/by-supplier/${supplierId}`);
  }  

  getProductsFromActiveSuppliers(page: number = 0, size: number = 20): Observable<PaginatedResponse<ProductItem>> {
    const params = { page: page.toString(), size: size.toString() };
    return this.http.get<PaginatedResponse<ProductItem>>(`${this.baseUrl}/by-supplier-open`, { params });
  }
}
