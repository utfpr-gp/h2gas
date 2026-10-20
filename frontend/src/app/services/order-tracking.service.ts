import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface OrderStatusChange {
  orderId: string;
  status: string;
}

/**
 * HU08: acompanha em tempo real os pedidos do cliente logado. Um EventSource por cliente, no canal
 * customer:{id} (o backend só deixa assinar o próprio); o JWT vai em ?token= porque o EventSource
 * nativo não manda header (fallback do F4, exclusivo de /api/sse/**). A reconexão após queda de
 * rede é a nativa do EventSource.
 */
@Injectable({ providedIn: 'root' })
export class OrderTrackingService {
  private eventSource?: EventSource;
  private readonly statusChange$ = new Subject<OrderStatusChange>();
  readonly onStatusChange: Observable<OrderStatusChange> = this.statusChange$.asObservable();

  constructor(private authService: AuthService) {}

  connect(customerId: string) {
    this.disconnect();
    const token = encodeURIComponent(this.authService.getToken() ?? '');
    const url = `${environment.apiUrl}/sse/subscribe/customer:${customerId}?token=${token}`;
    this.eventSource = this.createEventSource(url);
    this.eventSource.addEventListener('order-status-changed', (event) => {
      this.statusChange$.next(JSON.parse((event as MessageEvent).data));
    });
  }

  disconnect() {
    this.eventSource?.close();
    this.eventSource = undefined;
  }

  // ponto de troca para o spec usar um EventSource falso
  protected createEventSource(url: string): EventSource {
    return new EventSource(url);
  }
}
