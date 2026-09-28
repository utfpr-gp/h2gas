import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { FirstAccessAddressComponent } from './first-access-address.component';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';
import { UserService } from '../../../services/user.service';
import { LocationContextService } from '../../../services/location-context.service';

describe('FirstAccessAddressComponent', () => {
  let component: FirstAccessAddressComponent;
  let fixture: ComponentFixture<FirstAccessAddressComponent>;
  let addressServiceSpy: jasmine.SpyObj<AddressService>;
  let cityServiceSpy: jasmine.SpyObj<CityService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let locationContextService: LocationContextService;

  const patoBranco = { id: 1, name: 'Pato Branco', state: 'PR' };

  beforeEach(async () => {
    localStorage.clear();
    addressServiceSpy = jasmine.createSpyObj('AddressService', ['buscaViaCep', 'create', 'findByUserId']);
    cityServiceSpy = jasmine.createSpyObj('CityService', ['findAll']);
    userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    cityServiceSpy.findAll.and.returnValue(of([patoBranco] as any));
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'user-1' });

    await TestBed.configureTestingModule({
      imports: [FirstAccessAddressComponent],
      providers: [
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: CityService, useValue: cityServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideNgxMask(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FirstAccessAddressComponent);
    component = fixture.componentInstance;
    locationContextService = TestBed.inject(LocationContextService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveBuscarEnderecoPorCep', () => {
    addressServiceSpy.buscaViaCep.and.returnValue(
      of({ logradouro: 'Rua XV', bairro: 'Centro', localidade: 'Pato Branco' })
    );

    component.form.get('zipCode')?.setValue('85501-000');
    component.buscarCep();

    expect(component.cidadeEncontrada).toBeTrue();
    expect(component.form.get('streetName')?.value).toBe('Rua XV');
    expect(component.form.get('neighborhood')?.value).toBe('Centro');
    expect(component.form.get('cityId')?.value).toBe(1);
  });

  it('deveSalvarEnderecoEAtualizarLocationContext', () => {
    addressServiceSpy.create.and.returnValue(of({ id: 10 } as any));

    component.form.setValue({
      zipCode: '85501-000',
      streetName: 'Rua XV',
      neighborhood: 'Centro',
      number: '100',
      complement: '',
      cityId: 1,
    });
    component.cidadeEncontrada = true;

    component.salvar();

    expect(addressServiceSpy.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ userId: 'user-1', cityId: 1 })
    );
    expect(locationContextService.current()).toEqual({
      cityId: 1,
      cityName: 'Pato Branco',
      label: 'Rua XV, 100 · Pato Branco',
    });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/inicio']);
  });
});
