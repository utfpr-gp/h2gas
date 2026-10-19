import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { OrderService } from './order.service';
import { environment } from '../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('deveBuscarReorderPreviewDoPedido', () => {
    service.getReorderPreview('pedido-1').subscribe((preview) => {
      expect(preview.availableItems.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/pedido-1/reorder-preview`);
    expect(req.request.method).toBe('GET');
    req.flush({ availableItems: [{ productId: 5 }], unavailableItems: [] });
  });

  it('deveCancelarPedidoComMotivo', () => {
    service.cancelOrder('pedido-1', 'Sem entregador').subscribe((resposta) => {
      expect(resposta.customerNotified).toBeTrue();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/pedido-1/cancel`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ reason: 'Sem entregador' });
    req.flush({ status: 'Cancelado', customerNotified: true });
  });

  it('deveDespacharPedido', () => {
    service.assignDeliverer('pedido-1', 7).subscribe((pedido) => expect(pedido.delivererName).toBe('Carlos'));

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/pedido-1/assign-deliverer`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ delivererId: 7 });
    req.flush({ id: 'pedido-1', delivererName: 'Carlos' });
  });

  it('deveBuscarMinhasRotas', () => {
    service.myRoutes().subscribe((rotas) => expect(rotas.length).toBe(1));

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/my-routes`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'pedido-1' }]);
  });

  it('deveIniciarEntrega', () => {
    service.startDelivery('pedido-1').subscribe((pedido) => expect(pedido.status).toBe('Saiu para entrega'));

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/pedido-1/start-delivery`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 'pedido-1', status: 'Saiu para entrega' });
  });

  it('deveCancelarEntregaComMotivo', () => {
    service.cancelDelivery('pedido-1', 'Moto quebrou').subscribe((pedido) => expect(pedido.status).toBe('Em preparacao'));

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/pedido-1/cancel-delivery`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ reason: 'Moto quebrou' });
    req.flush({ id: 'pedido-1', status: 'Em preparacao' });
  });

  it('deveBuscarHistoricoDeEntregas', () => {
    service.deliveryHistory().subscribe((h) => {
      expect(h.deliveredToday).toBe(12);
      expect(h.items[0].kind).toBe('DELIVERED');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/my-deliveries/history`);
    expect(req.request.method).toBe('GET');
    req.flush({ deliveredToday: 12, items: [{ orderId: 'pedido-1', kind: 'DELIVERED' }] });
  });

  it('deveConfirmarEntrega', () => {
    service.completeDelivery('pedido-1').subscribe((pedido) => expect(pedido.status).toBe('Entregue'));

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/pedido-1/complete-delivery`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 'pedido-1', status: 'Entregue' });
  });
});
