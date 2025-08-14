import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { OrderItemResponse } from '../models/order-item.model';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class OrderItemService extends AbstractService<OrderItemResponse> {
  override baseUrl: string = `${environment.apiUrl}/orders`

  constructor(protected override http: HttpClient) {
    super(http);
  }
}
