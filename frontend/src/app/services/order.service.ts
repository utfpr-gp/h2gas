import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { environment } from '../../environments/environment';
import { CancelOrderResponse, OrderPreview, OrderPreviewRequest, OrderRequest, OrderResponse, ReorderPreview } from '../models/order.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { OrderHistoryFilter, OrderHistorySummary } from '../models/order-history';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../models/paginated-response';
import { DeliveryHistory } from '../models/delivery-history';

@Injectable({
  providedIn: 'root'
})
export class OrderService extends AbstractService<OrderResponse> {
  override baseUrl: string = `${environment.apiUrl}/orders`

  constructor(protected override http: HttpClient) {
    super(http);
  }

  // F7 fatia 6: histórico do fornecedor do token, filtrado e contado no backend
  listMyHistory(filter: OrderHistoryFilter & { page?: number; size?: number }): Observable<PaginatedResponse<OrderResponse>> {
    let params = this.historyParams(filter);
    if (filter.page != null) params = params.set('page', filter.page);
    if (filter.size != null) params = params.set('size', filter.size);
    return this.http.get<PaginatedResponse<OrderResponse>>(`${this.baseUrl}/mine/history`, { params });
  }

  getMyHistorySummary(filter: OrderHistoryFilter): Observable<OrderHistorySummary> {
    return this.http.get<OrderHistorySummary>(`${this.baseUrl}/mine/history/summary`, { params: this.historyParams(filter) });
  }

  private historyParams(filter: OrderHistoryFilter): HttpParams {
    let params = new HttpParams();
    for (const key of ['status', 'from', 'to', 'payment', 'mode'] as const) {
      if (filter[key]) params = params.set(key, filter[key]!);
    }
    if (filter.q?.trim()) params = params.set('q', filter.q.trim());
    return params;
  }

  createOrder(data: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(this.baseUrl, data);
  }

  previewOrder(data: OrderPreviewRequest): Observable<OrderPreview> {
    return this.http.post<OrderPreview>(`${this.baseUrl}/preview`, data);
  }

  assignDeliverer(orderId: string, delivererId: number): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.baseUrl}/${orderId}/assign-deliverer`, { delivererId });
  }

  myRoutes(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.baseUrl}/my-routes`);
  }

  startDelivery(orderId: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.baseUrl}/${orderId}/start-delivery`, {});
  }

  cancelDelivery(orderId: string, reason: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.baseUrl}/${orderId}/cancel-delivery`, { reason });
  }

  deliveryHistory(): Observable<DeliveryHistory> {
    return this.http.get<DeliveryHistory>(`${this.baseUrl}/my-deliveries/history`);
  }

  completeDelivery(orderId: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.baseUrl}/${orderId}/complete-delivery`, {});
  }

  cancelOrder(orderId: string, reason: string): Observable<CancelOrderResponse> {
    return this.http.patch<CancelOrderResponse>(`${this.baseUrl}/${orderId}/cancel`, { reason });
  }

  getReorderPreview(orderId: string): Observable<ReorderPreview> {
    return this.http.get<ReorderPreview>(`${this.baseUrl}/${orderId}/reorder-preview`);
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
