import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { SupplierService } from './supplier.service';
import { environment } from '../../environments/environment';

describe('SupplierService', () => {
  let service: SupplierService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SupplierService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('deveBuscarDashboardDoFornecedor', () => {
    service.getDashboard().subscribe((d) => expect(d.totalOrders).toBe(84));

    const req = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/dashboard`);
    expect(req.request.method).toBe('GET');
    req.flush({ month: '2026-10', totalOrders: 84 });
  });

  it('deveEnviarMesSelecionado', () => {
    service.getDashboard('2026-09').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/dashboard?month=2026-09`);
    expect(req.request.method).toBe('GET');
    req.flush({ month: '2026-09' });
  });

  it('deveBuscarConfiguracoesDoFornecedorLogado', () => {
    service.getMySettings().subscribe((c) => expect(c.lowStockThreshold).toBe(8));

    const req = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/settings`);
    expect(req.request.method).toBe('GET');
    req.flush({ openingTime: '08:00', closingTime: '18:00', averageDeliveryTime: 30, lowStockThreshold: 8 });
  });

  it('deveSalvarConfiguracoes', () => {
    const settings = { openingTime: '09:00', closingTime: '02:00', averageDeliveryTime: 40, lowStockThreshold: 5 };
    service.updateMySettings(settings).subscribe((c) => expect(c).toEqual(settings));

    const req = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/settings`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(settings);
    req.flush(settings);
  });
});
