import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { CategoryProduct } from '../models/category-product';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CategoryRequest, SupplierCategory } from '../models/supplier-category';

@Injectable({
  providedIn: 'root'
})
export class CategoryProductService extends AbstractService<CategoryProduct> {
  override baseUrl: string = `${environment.apiUrl}/category-products`


  constructor(protected override http: HttpClient) { 
    super(http);
  }

  // HU19: as categorias que a loja do token usa (a lista de todas é o findAll herdado, público)
  listMine(): Observable<SupplierCategory[]> {
    return this.http.get<SupplierCategory[]>(`${this.baseUrl}/mine`);
  }

  createCategory(request: CategoryRequest): Observable<CategoryProduct> {
    return this.http.post<CategoryProduct>(this.baseUrl, request);
  }

  updateCategory(id: number, request: CategoryRequest): Observable<CategoryProduct> {
    return this.http.put<CategoryProduct>(`${this.baseUrl}/${id}`, request);
  }
}
