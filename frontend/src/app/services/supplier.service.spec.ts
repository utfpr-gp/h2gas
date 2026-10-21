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

  it('deveEnviarFiltrosNaListagem', () => {
    service.listForApproval({ active: false, q: ' gás ', page: 2, size: 10 }).subscribe((r) => expect(r.totalElements).toBe(1));

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/suppliers`);
    expect(req.request.params.get('active')).toBe('false');
    expect(req.request.params.get('q')).toBe('gás');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ data: [], page: 2, size: 10, totalElements: 1, totalPages: 3, hasNext: false, hasPrevious: true });
  });

  it('deveOmitirStatusEBuscaQuandoAusentes', () => {
    service.listForApproval({ page: 0 }).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/suppliers`);
    expect(req.request.params.has('active')).toBeFalse();
    expect(req.request.params.has('q')).toBeFalse();
    req.flush({ data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  });
});
