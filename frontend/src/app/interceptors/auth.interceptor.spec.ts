import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { authInterceptor } from './auth.interceptor';

// JWT de mentira só com o exp (o interceptor só lê a validade)
const jwtCom = (expEmSegundos: number) =>
  `${btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))}.${btoa(JSON.stringify({ sub: 'u', exp: expEmSegundos }))}.assinatura`;

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  const agora = () => Math.floor(Date.now() / 1000);

  beforeEach(() => {
    localStorage.clear();
    routerSpy = jasmine.createSpyObj('Router', ['navigate'], { url: '/resumo-pedido?modo=compra-direta' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deveEnviarTokenValido', () => {
    localStorage.setItem('access_token', jwtCom(agora() + 3600));

    http.get('/api/x').subscribe();

    expect(httpMock.expectOne('/api/x').request.headers.get('Authorization')).toMatch(/^Bearer /);
  });

  // HU13 Cenário 12: token vencido não vai junto, e a vitrine responde como para visitante
  it('naoDeveEnviarTokenVencido', () => {
    localStorage.setItem('access_token', jwtCom(agora() - 60));

    http.get('/api/suppliers/actives/open').subscribe();

    expect(httpMock.expectOne('/api/suppliers/actives/open').request.headers.has('Authorization')).toBeFalse();
  });

  it('deveMandarParaOLoginComRetornoEm401', () => {
    localStorage.setItem('access_token', jwtCom(agora() + 3600));

    http.get('/api/orders/preview').subscribe({ error: () => {} });
    httpMock.expectOne('/api/orders/preview').flush('Token inválido', { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/resumo-pedido?modo=compra-direta' } });
  });
});
