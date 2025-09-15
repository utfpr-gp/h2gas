import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { ProductType } from '../models/product-type';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ProductTypeService extends AbstractService<ProductType> {
  override baseUrl: string = `${environment.apiUrl}/product-types`

  constructor(protected override http: HttpClient) {
    super(http);
  }
}
