import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { CategoryProduct } from '../models/category-product';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class CategoryProductService extends AbstractService<CategoryProduct> {
  override baseUrl: string = `${environment.apiUrl}/category-products`


  constructor(protected override http: HttpClient) { 
    super(http);
  }
}
