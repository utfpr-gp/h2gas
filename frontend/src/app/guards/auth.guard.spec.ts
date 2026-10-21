import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router } from '@angular/router';

import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';
import { SupplierService } from '../services/supplier.service';

describe('authGuard', () => {
  let routerSpy: jasmine.SpyObj<Router>;
  let authSpy: jasmine.SpyObj<AuthService>;
  const executeGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    authSpy = jasmine.createSpyObj('AuthService', ['isLogged']);
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: UserService, useValue: jasmine.createSpyObj('UserService', ['getUserLocalStorage']) },
        { provide: SupplierService, useValue: jasmine.createSpyObj('SupplierService', ['getActiveSupplier']) },
      ],
    });
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });

  // HU13: quem não entrou vai para o login e volta para a página pedida
  it('deveMandarParaOLoginComReturnUrl', () => {
    authSpy.isLogged.and.returnValue(false);

    const result = executeGuard({ data: {} } as any, { url: '/resumo-pedido?modo=compra-direta' } as any);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/resumo-pedido?modo=compra-direta' } });
  });
});
