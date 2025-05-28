import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { ProductItem } from '../models/product-item';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ProductItemService extends AbstractService<ProductItem> {
  override baseUrl: string = `${environment.apiUrl}/product-items`

  constructor(protected override http: HttpClient) {
    super(http);
  }
}
