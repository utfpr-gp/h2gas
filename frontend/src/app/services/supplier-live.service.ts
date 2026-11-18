import { Injectable, Injector } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { orderCode } from '../utils/whatsapp-link';
import { UserService } from './user.service';

export interface SupplierLiveEvent {
  type: 'order-placed' | 'order-status-changed';
  orderId: string;
  status?: string;
}

/**
 * HU27 (tempo real da loja): um EventSource no canal supplier:{id} (o backend só deixa assinar o
 * próprio). Pedido novo vira o aviso "Novo pedido #código"; as telas da loja recarregam em changes().
 * O JWT vai em ?token= porque o EventSource nativo não manda header (como a HU08).
 */
@Injectable({ providedIn: 'root' })
export class SupplierLiveService {
  private eventSource?: EventSource;
  private supplierId?: string;
  private readonly changes$ = new Subject<SupplierLiveEvent>();

  // dependências buscadas só ao conectar: o AuthService cria este serviço junto com o login (para o logout)
  constructor(private injector: Injector) {}

  /** Conecta na primeira assinatura (só para a loja) e devolve os eventos do canal. */
  changes(): Observable<SupplierLiveEvent> {
    this.ensureConnected();
    return this.changes$.asObservable();
  }

  disconnect() {
    this.eventSource?.close();
    this.eventSource = undefined;
    this.supplierId = undefined;
  }

  private ensureConnected() {
    const user = this.injector.get(UserService).getUserLocalStorage();
    if (user?.userType !== 'SUPPLIER' || !user.id) return;
    if (this.eventSource && this.supplierId === user.id) return;
    this.disconnect();
    // lido direto do storage: o AuthService desconecta este serviço no logout (evita dependência circular)
    const token = encodeURIComponent(localStorage.getItem('access_token') ?? '');
    this.supplierId = user.id;
    this.eventSource = this.createEventSource(`${environment.apiUrl}/sse/subscribe/supplier:${user.id}?token=${token}`);
    this.eventSource.addEventListener('order-placed', (event) => {
      const data = JSON.parse((event as MessageEvent).data);
      this.injector.get(ToastrService).info(`Novo pedido #${orderCode(data.orderId)}`);
      this.changes$.next({ type: 'order-placed', orderId: data.orderId });
    });
    this.eventSource.addEventListener('order-status-changed', (event) => {
      const data = JSON.parse((event as MessageEvent).data);
      this.changes$.next({ type: 'order-status-changed', orderId: data.orderId, status: data.status });
    });
  }

  // ponto de troca para o spec usar um EventSource falso
  protected createEventSource(url: string): EventSource {
    return new EventSource(url);
  }
}
