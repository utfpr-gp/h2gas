import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { CategoryProduct } from '../models/category-product';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../models/paginated-response';

@Injectable({
  providedIn: 'root'
})
export class CategoryProductService extends AbstractService<CategoryProduct> {
  override baseUrl: string = `${environment.apiUrl}/category-products`


  constructor(protected override http: HttpClient) { 
    super(http);
  }

  findBySupplierId(
    supplierId: string,
    page: number = 0,
    size: number = 10
  ): Observable<PaginatedResponse<CategoryProduct>> {
    let params = `?page=${page}&size=${size}`;
  
    return this.http.get<PaginatedResponse<CategoryProduct>>(
      `${this.baseUrl}/by-supplier/${supplierId}${params}`
    );
  }
}
