import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { environment } from '../../environments/environment';
import { OrderRequest, OrderResponse } from '../models/order.model';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../models/paginated-response';

@Injectable({
  providedIn: 'root'
})
export class OrderService extends AbstractService<OrderResponse> {
  override baseUrl: string = `${environment.apiUrl}/orders`

  constructor(protected override http: HttpClient) {
    super(http);
  }

  createOrder(data: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(this.baseUrl, data);
  }

  findByCustomerName(customerId: string): Observable<PaginatedResponse<OrderResponse>> {
    return this.http.get<PaginatedResponse<OrderResponse>>(
      `${this.baseUrl}/by-customer/${customerId}`
    );
  }

}
