import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { UserService } from './user.service';
import { environment } from '../../environments/environment';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const criar = () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  };

  beforeEach(() => localStorage.clear());

  it('should be created', () => {
    criar();
    expect(service).toBeTruthy();
  });

  it('deveComecarComOUsuarioGuardadoNoNavegador', () => {
    localStorage.setItem('user', JSON.stringify({ id: 'u1', name: 'Maria Souza' }));
    criar();
    let atual: any;

    service.currentUser$.subscribe((u) => (atual = u));

    expect(atual.name).toBe('Maria Souza');
  });

  it('deveComecarSemUsuarioQuandoNaoHaNadaGuardado', () => {
    criar();
    let atual: any = 'x';

    service.currentUser$.subscribe((u) => (atual = u));

    expect(atual).toBeNull();
  });

  it('deveEmitirOUsuarioAtualAoSalvar', () => {
    criar();
    const recebidos: any[] = [];
    service.currentUser$.subscribe((u) => recebidos.push(u));

    service.setCurrentUser({ id: 'u1', name: 'Maria Lima' } as any);

    expect(recebidos[recebidos.length - 1].name).toBe('Maria Lima');
    expect(JSON.parse(localStorage.getItem('user')!).name).toBe('Maria Lima');
  });

  it('deveEditarOProprioUsuarioPeloId', () => {
    criar();

    service.updateUser('u1', { name: 'Maria' } as any).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/users/u1`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 'u1', name: 'Maria' });
    httpMock.verify();
  });

  it('devePedirEConfirmarATrocaDoProprioEmail', () => {
    criar();
    service.requestEmailChange('novo@h2gas.local').subscribe();
    const pedido = httpMock.expectOne(`${environment.apiUrl}/users/me/email-change`);
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({ newEmail: 'novo@h2gas.local' });
    pedido.flush(null, { status: 202, statusText: 'Accepted' });

    service.confirmEmailChange('123456').subscribe((r) => expect(r.token).toBe('jwt-novo'));
    const confirma = httpMock.expectOne(`${environment.apiUrl}/users/me/email-change/confirm`);
    expect(confirma.request.method).toBe('POST');
    expect(confirma.request.body).toEqual({ code: '123456' });
    confirma.flush({ token: 'jwt-novo' });
    httpMock.verify();
  });
});
