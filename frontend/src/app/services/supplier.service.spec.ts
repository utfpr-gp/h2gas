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

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/suppliers/mine/dashboard` && r.params.get('month') === '2026-09');
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

  it('deveBuscarPerfilDaLoja', () => {
    service.getMyProfile().subscribe((p) => expect(p.cnpj).toBe('12345678000101'));
    const get = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/profile`);
    expect(get.request.method).toBe('GET');
    get.flush({ name: 'Gás Centro', phone: '41991110000', cnpj: '12345678000101' });

    service.updateMyProfile({ name: 'Gás Centro', phone: '41991110000', description: 'Desde 1990' }).subscribe();
    const put = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/profile`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ name: 'Gás Centro', phone: '41991110000', description: 'Desde 1990' });
    put.flush({});
  });

  it('deveEnviarLogoComoFormData', () => {
    const arquivo = new File([new Uint8Array([1, 2, 3])], 'logo.png', { type: 'image/png' });
    service.uploadMyLogo(arquivo).subscribe((r) => expect(r.logoUrl).toBe('https://cdn/logo.png'));

    const req = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/logo`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('file')).toBe(arquivo);
    req.flush({ logoUrl: 'https://cdn/logo.png' });
  });

  it('deveRemoverLogo', () => {
    service.removeMyLogo().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/suppliers/mine/logo`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('deveEnviarFiltrosDoDashboard', () => {
    service.getDashboard('2026-10', { payment: 'PIX', mode: 'PICKUP', typeId: 2, categoryId: 7, productId: 4 }).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/suppliers/mine/dashboard`);
    expect(req.request.params.get('month')).toBe('2026-10');
    expect(req.request.params.get('payment')).toBe('PIX');
    expect(req.request.params.get('mode')).toBe('PICKUP');
    expect(req.request.params.get('typeId')).toBe('2');
    expect(req.request.params.get('categoryId')).toBe('7');
    expect(req.request.params.get('productId')).toBe('4');
    req.flush({ month: '2026-10' });

    service.getDashboard('2026-10').subscribe();
    const semFiltro = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/suppliers/mine/dashboard`);
    expect(semFiltro.request.params.keys()).toEqual(['month']);
    semFiltro.flush({ month: '2026-10' });
  });

  it('deveBuscarLojasAbertasDaCidadeComBusca', () => {
    service.getAllSupplierOpenAndActive(1, 10, 7, ' gás ').subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/suppliers/actives/open`);
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('cityId')).toBe('7');
    expect(req.request.params.get('q')).toBe('gás');
    req.flush({ data: [], page: 1, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: true });

    service.getAllSupplierOpenAndActive(0, 10, 7).subscribe();
    const semBusca = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/suppliers/actives/open`);
    expect(semBusca.request.params.has('q')).toBeFalse();
    semBusca.flush({ data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  });
});
