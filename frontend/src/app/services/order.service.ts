import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { environment } from '../../environments/environment';
import { OrderPreview, OrderPreviewRequest, OrderRequest, OrderResponse } from '../models/order.model';
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

  previewOrder(data: OrderPreviewRequest): Observable<OrderPreview> {
    return this.http.post<OrderPreview>(`${this.baseUrl}/preview`, data);
  }

  findByCustomerId(customerId: string, page: number = 0, size: number = 5, status?: string): Observable<PaginatedResponse<OrderResponse>> {
    const params: any = { page, size };
    if (status && status !== 'TODOS') {
      params.status = status;
    }
  
    return this.http.get<PaginatedResponse<OrderResponse>>(
      `${this.baseUrl}/by-customer/${customerId}`,
      { params }
    );
  }

  findBySupplierId(
    supplierId: string,
    status?: string,
    page: number = 0,
    size: number = 10
  ): Observable<PaginatedResponse<OrderResponse>> {
    let params = `?page=${page}&size=${size}`;
    if (status && status !== 'ALL') {
      params += `&status=${status}`;
    }
  
    return this.http.get<PaginatedResponse<OrderResponse>>(
      `${this.baseUrl}/by-supplier/${supplierId}${params}`
    );
  }

  findByExcludeStatus(
    supplierId: string,
    excludeStatuses?: string[],
    page: number = 0,
    size: number = 10
  ): Observable<PaginatedResponse<OrderResponse>> {
    let params = `?page=${page}&size=${size}`;
    if (excludeStatuses && excludeStatuses.length > 0) {
      params += `&excludeStatuses=${excludeStatuses.join(',')}`;
    }
  
    return this.http.get<PaginatedResponse<OrderResponse>>(
      `${this.baseUrl}/${supplierId}/exclude-status${params}`
    );
  }

  updateStatusOrder(data: any, id: string) {
    return this.http.patch(`${this.baseUrl}/${id}/update-order-status`, data);
  }

}
