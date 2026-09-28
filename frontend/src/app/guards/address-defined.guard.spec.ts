import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router } from '@angular/router';
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
  let locationContextService: LocationContextService;

  beforeEach(() => {
    localStorage.clear();
    addressServiceSpy = jasmine.createSpyObj('AddressService', ['findByUserId']);
    userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'user-1' });

    TestBed.configureTestingModule({
      providers: [
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    locationContextService = TestBed.inject(LocationContextService);
  });

  it('deveRedirecionarParaFirstAccessAddressSeUsuarioNaoTiverEndereco', (done) => {
    addressServiceSpy.findByUserId.and.returnValue(throwError(() => new Error('404')));

    const result = executeGuard({} as any, {} as any);

    (result as any).subscribe((canActivate: boolean) => {
      expect(canActivate).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/primeiro-acesso-endereco']);
      done();
    });
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
});
