import { TestBed } from '@angular/core/testing';

import { OrderTrackingService, OrderStatusChange } from './order-tracking.service';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

// HU08: um EventSource por cliente, no canal customer:{id}, com o JWT em ?token= (fallback do F4)
describe('OrderTrackingService', () => {
  let service: OrderTrackingService;
  let abertos: FakeEventSource[];

  class FakeEventSource {
    listeners: Record<string, (event: MessageEvent) => void> = {};
    close = jasmine.createSpy('close');
    constructor(public url: string) {}
    addEventListener(type: string, listener: (event: MessageEvent) => void) {
      this.listeners[type] = listener;
    }
    emitir(type: string, data: unknown) {
      this.listeners[type]?.({ data: JSON.stringify(data) } as MessageEvent);
    }
  }

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken']);
    authServiceSpy.getToken.and.returnValue('jwt-123');
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: authServiceSpy }] });
    service = TestBed.inject(OrderTrackingService);

    abertos = [];
    spyOn<any>(service, 'createEventSource').and.callFake((url: string) => {
      const fake = new FakeEventSource(url);
      abertos.push(fake);
      return fake;
    });
  });

  it('deveAbrirEventSourceComCanalDoClienteEToken', () => {
    service.connect('cliente-1');

    expect(abertos.length).toBe(1);
    expect(abertos[0].url).toBe(`${environment.apiUrl}/sse/subscribe/customer:cliente-1?token=jwt-123`);
  });

  it('deveEmitirEventoQuandoStatusMudar', () => {
    const recebidos: OrderStatusChange[] = [];
    service.onStatusChange.subscribe((change: OrderStatusChange) => recebidos.push(change));
    service.connect('cliente-1');

    abertos[0].emitir('order-status-changed', { orderId: 'pedido-1', status: 'Saiu para entrega' });

    expect(recebidos).toEqual([{ orderId: 'pedido-1', status: 'Saiu para entrega' }]);
  });

  it('deveFecharConexaoAnteriorAoReconectar', () => {
    service.connect('cliente-1');
    service.connect('cliente-1');

    expect(abertos.length).toBe(2);
    expect(abertos[0].close).toHaveBeenCalled();
    expect(abertos[1].close).not.toHaveBeenCalled();
  });

  it('deveFecharConexaoAoDesconectar', () => {
    service.connect('cliente-1');

    service.disconnect();
    service.disconnect();

    expect(abertos[0].close).toHaveBeenCalledTimes(1);
  });
});
