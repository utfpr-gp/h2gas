import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AdminService } from './admin.service';
import { environment } from '../../environments/environment';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiUrl}/admin`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deveListarUsuariosComFiltroBuscaEPagina', () => {
    service.listUsers({ type: 'CUSTOMER', q: 'souza', page: 2, size: 10 }).subscribe((p) => expect(p.totalElements).toBe(1));

    const req = httpMock.expectOne((r) => r.url === `${base}/users`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('type')).toBe('CUSTOMER');
    expect(req.request.params.get('q')).toBe('souza');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ data: [], page: 2, size: 10, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: true });
  });

  it('naoDeveMandarFiltroVazio', () => {
    service.listUsers({ q: '  ', page: 0 }).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${base}/users`);
    expect(req.request.params.has('type')).toBeFalse();
    expect(req.request.params.has('q')).toBeFalse();
    req.flush({ data: [] });
  });

  it('deveCriarUsuario', () => {
    const novo = { name: 'Ana', email: 'ana@h2gas.local', password: 'senha123', phoneNumber: '46999990000', userType: 'ADMIN' as const };
    service.createUser(novo).subscribe((u) => expect(u.id).toBe('u-1'));

    const req = httpMock.expectOne(`${base}/users`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(novo);
    req.flush({ id: 'u-1', ...novo, enabled: true });
  });

  it('deveAtivarOuDesativarConta', () => {
    service.setEnabled('u-1', false).subscribe();

    const req = httpMock.expectOne(`${base}/users/u-1/enabled`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ enabled: false });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('deveBuscarOPainel', () => {
    service.dashboard().subscribe((d) => expect(d.pendingSuppliers).toBe(2));

    const req = httpMock.expectOne(`${base}/dashboard`);
    expect(req.request.method).toBe('GET');
    req.flush({ pendingSuppliers: 2 });
  });
});
