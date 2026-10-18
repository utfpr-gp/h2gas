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
});
