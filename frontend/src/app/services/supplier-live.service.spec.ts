import { TestBed } from '@angular/core/testing';
import { ToastrService } from 'ngx-toastr';
import { SupplierLiveService, SupplierLiveEvent } from './supplier-live.service';
import { UserService } from './user.service';

// HU27 (tempo real da loja): um EventSource no canal supplier:{id}; pedido novo vira aviso e recarga
class FakeEventSource {
  listeners: Record<string, (e: MessageEvent) => void> = {};
  closed = false;
  constructor(public url: string) {}
  addEventListener(name: string, fn: (e: MessageEvent) => void) { this.listeners[name] = fn; }
  emit(name: string, data: object) { this.listeners[name]?.({ data: JSON.stringify(data) } as MessageEvent); }
  close() { this.closed = true; }
}

describe('SupplierLiveService', () => {
  let service: SupplierLiveService;
  let toastr: jasmine.SpyObj<ToastrService>;
  let user: any;
  let sources: FakeEventSource[];

  beforeEach(() => {
    sources = [];
    user = { id: 'loja-1', userType: 'SUPPLIER' };
    toastr = jasmine.createSpyObj('ToastrService', ['info']);
    localStorage.setItem('access_token', 'tok en');
    TestBed.configureTestingModule({
      providers: [
        { provide: ToastrService, useValue: toastr },
        { provide: UserService, useValue: { getUserLocalStorage: () => user } },
      ],
    });
    service = TestBed.inject(SupplierLiveService);
    spyOn<any>(service, 'createEventSource').and.callFake((url: string) => {
      const es = new FakeEventSource(url);
      sources.push(es);
      return es;
    });
  });

  afterEach(() => localStorage.removeItem('access_token'));

  it('deveConectarNoCanalDaLojaComOToken', () => {
    service.changes().subscribe();
    service.changes().subscribe();

    expect(sources.length).toBe(1);
    expect(sources[0].url).toContain('/sse/subscribe/supplier:loja-1?token=tok%20en');
  });

  it('deveAvisarOPedidoNovoEEmitirAsMudancas', () => {
    const recebidos: SupplierLiveEvent[] = [];
    service.changes().subscribe((e: SupplierLiveEvent) => recebidos.push(e));

    sources[0].emit('order-placed', { orderId: 'c6423ce8-fc0b' });
    sources[0].emit('order-status-changed', { orderId: 'c6423ce8-fc0b', status: 'Entregue' });

    expect(toastr.info).toHaveBeenCalledOnceWith('Novo pedido #c6423ce8');
    expect(recebidos.map((e) => e.type)).toEqual(['order-placed', 'order-status-changed']);
  });

  it('naoDeveConectarQuemNaoELoja', () => {
    user = { id: 'cli-1', userType: 'CUSTOMER' };
    service.changes().subscribe();

    expect(sources.length).toBe(0);
  });

  it('deveFecharAoDesconectar', () => {
    service.changes().subscribe();
    service.disconnect();

    expect(sources[0].closed).toBeTrue();
    service.changes().subscribe();
    expect(sources.length).toBe(2);
  });
});
