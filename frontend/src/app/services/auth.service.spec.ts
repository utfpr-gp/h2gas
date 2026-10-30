import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { LocationContextService } from './location-context.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let locationContextService: LocationContextService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    locationContextService = TestBed.inject(LocationContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('deveLimparEnderecoAtivoAoFazerLogout', () => {
    localStorage.setItem('access_token', 'fake-token');
    localStorage.setItem('user', JSON.stringify({ id: 'user-1' }));
    locationContextService.setAddress({ cityId: 1, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });

    service.logout();

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(locationContextService.current()).toBeNull();
    expect(localStorage.getItem('h2gas.activeAddress')).toBeNull();
  });

  it('deveSolicitarCodigoDeAcesso', () => {
    service.requestLoginToken('cliente@h2gas.local').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login/token/request`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'cliente@h2gas.local', channel: 'EMAIL' });
    req.flush(null);
  });

  // HU26
  it('deveSolicitarCodigoPeloCelularEConsultarOsCanais', () => {
    service.requestLoginToken('cliente@h2gas.local', 'PHONE').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login/token/request`);
    expect(req.request.body).toEqual({ email: 'cliente@h2gas.local', channel: 'PHONE' });
    req.flush(null);

    service.getLoginChannels().subscribe((c) => expect(c.phoneChannel).toBe('SMS'));
    httpMock.expectOne(`${environment.apiUrl}/auth/login/channels`).flush({ phone: true, phoneChannel: 'SMS' });
  });

  it('deveValidarCodigoEReceberToken', () => {
    service.validateLoginToken('cliente@h2gas.local', '123456')
      .subscribe((resposta) => expect(resposta.token).toBe('jwt-do-codigo'));

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login/token/validate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'cliente@h2gas.local', code: '123456' });
    req.flush({ token: 'jwt-do-codigo' });
  });

  // HU13: o carrinho é limpo ao sair (e não mais ao entrar), para o visitante levar o carrinho ao login
  it('deveLimparOCarrinhoAoSair', () => {
    localStorage.setItem('cart-product', JSON.stringify([{ id: 1, quantitySelect: 2 }]));
    sessionStorage.setItem('buy-now-item', JSON.stringify({ id: 9, quantitySelect: 1 }));

    service.logout();

    expect(JSON.parse(localStorage.getItem('cart-product') ?? '[]')).toEqual([]);
    expect(sessionStorage.getItem('buy-now-item')).toBeNull();
  });

  // HU13 Cenário 12: sessão vencida vira visitante sem perder carrinho nem cidade
  it('deveDescartarSessaoVencidaSemLimparOCarrinho', () => {
    const vencido = `${btoa('{"alg":"HS256"}')}.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 60 }))}.x`;
    localStorage.setItem('access_token', vencido);
    localStorage.setItem('user', JSON.stringify({ id: 'u1' }));
    localStorage.setItem('cart-product', JSON.stringify([{ id: 1, quantitySelect: 1 }]));
    locationContextService.setAddress({ cityId: 3, cityName: 'Curitiba', label: 'Curitiba' });

    service.dropExpiredSession();

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(JSON.parse(localStorage.getItem('cart-product')!).length).toBe(1);
    expect(locationContextService.current()).not.toBeNull();
  });
});
