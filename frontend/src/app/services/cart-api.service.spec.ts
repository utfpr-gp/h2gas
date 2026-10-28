import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CartApiService } from './cart-api.service';
import { environment } from '../../environments/environment';

describe('CartApiService', () => {
  let service: CartApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CartApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deveEnviarAsLinhasEAModalidadeAoPreview', () => {
    let resposta: any;
    service.preview([{ productId: 5, quantity: 2 }], 'PICKUP').subscribe((r) => (resposta = r));

    const req = http.expectOne(`${environment.apiUrl}/cart/preview`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ items: [{ productId: 5, quantity: 2 }], deliveryMode: 'PICKUP' });
    req.flush({ supplierId: 'loja-a', supplierName: 'Gás', items: [], subtotal: 0 });

    expect(resposta.supplierId).toBe('loja-a');
  });
});
