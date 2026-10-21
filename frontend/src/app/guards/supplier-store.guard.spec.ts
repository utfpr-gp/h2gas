import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router, UrlTree, provideRouter } from '@angular/router';

import { supplierStoreGuard } from './supplier-store.guard';
import { UserService } from '../services/user.service';

describe('supplierStoreGuard', () => {
  const executeGuard: CanActivateFn = (...params) => TestBed.runInInjectionContext(() => supplierStoreGuard(...params));
  let userSpy: jasmine.SpyObj<UserService>;

  beforeEach(() => {
    userSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: UserService, useValue: userSpy }] });
  });

  it('deveLevarFornecedorDeMeusDadosParaMinhaLoja', () => {
    userSpy.getUserLocalStorage.and.returnValue({ id: 'f1', userType: 'SUPPLIER' });

    const result = executeGuard({} as any, { url: '/meus-dados' } as any);

    expect(result instanceof UrlTree).toBeTrue();
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/minha-loja');
  });

  it('deveDeixarOutrosPerfisEmMeusDados', () => {
    for (const userType of ['CUSTOMER', 'DELIVERER', 'ADMIN']) {
      userSpy.getUserLocalStorage.and.returnValue({ id: 'u1', userType });
      expect(executeGuard({} as any, { url: '/meus-dados' } as any)).withContext(userType).toBeTrue();
    }
  });
});
