import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of, throwError } from 'rxjs';

import { addressDefinedGuard } from './address-defined.guard';
import { AddressService } from '../services/address.service';
import { UserService } from '../services/user.service';
import { LocationContextService } from '../services/location-context.service';

describe('addressDefinedGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => addressDefinedGuard(...guardParameters));

  let addressServiceSpy: jasmine.SpyObj<AddressService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let locationContextService: LocationContextService;

  beforeEach(() => {
    localStorage.clear();
    addressServiceSpy = jasmine.createSpyObj('AddressService', ['findByUserId']);
    userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['error']);

    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'user-1' });

    TestBed.configureTestingModule({
      providers: [
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    });

    locationContextService = TestBed.inject(LocationContextService);
  });

  it('deveRedirecionarParaFirstAccessAddressSeUsuarioNaoTiverEndereco', (done) => {
    addressServiceSpy.findByUserId.and.returnValue(throwError(() => ({ status: 404 })));

    const result = executeGuard({} as any, {} as any);

    (result as any).subscribe((canActivate: boolean) => {
      expect(canActivate).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/primeiro-acesso-endereco']);
      done();
    });
  });

  it('naoDeveRedirecionarParaFirstAccessQuandoErroNaoForDeEnderecoNaoEncontrado', (done) => {
    addressServiceSpy.findByUserId.and.returnValue(throwError(() => ({ status: 500 })));

    const result = executeGuard({} as any, {} as any);

    (result as any).subscribe((canActivate: boolean) => {
      expect(canActivate).toBeFalse();
      expect(routerSpy.navigate).not.toHaveBeenCalled();
      expect(toastrSpy.error).toHaveBeenCalled();
      done();
    });
  });

  it('deveLiberarAdminSemExigirEndereco', () => {
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'admin-1', userType: 'ADMIN' });

    const result = executeGuard({} as any, {} as any);

    expect(result).toBeTrue();
    expect(addressServiceSpy.findByUserId).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('deveLiberarNavegacaoSeEnderecoJaExiste', (done) => {
    addressServiceSpy.findByUserId.and.returnValue(
      of({ cityId: 1, cityName: 'Pato Branco', streetName: 'Rua XV', number: '100' })
    );

    const result = executeGuard({} as any, {} as any);

    (result as any).subscribe((canActivate: boolean) => {
      expect(canActivate).toBeTrue();
      expect(locationContextService.current()).toEqual({
        cityId: 1,
        cityName: 'Pato Branco',
        label: 'Rua XV, 100 · Pato Branco',
      });
      done();
    });
  });

  // HU13: cliente sem endereço que veio do checkout volta para ele depois de cadastrar
  it('deveLevarOReturnUrlAoPrimeiroEndereco', (done) => {
    addressServiceSpy.findByUserId.and.returnValue(throwError(() => ({ status: 404 })));

    const result = executeGuard({} as any, { url: '/resumo-pedido' } as any);

    (result as any).subscribe(() => {
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/primeiro-acesso-endereco'], { queryParams: { returnUrl: '/resumo-pedido' } });
      done();
    });
  });
});
