import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { FirstAccessAddressComponent } from './first-access-address.component';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';
import { LocationContextService } from '../../../services/location-context.service';

describe('FirstAccessAddressComponent', () => {
  let component: FirstAccessAddressComponent;
  let fixture: ComponentFixture<FirstAccessAddressComponent>;
  let routerSpy: jasmine.SpyObj<Router>;
  let locationContextService: LocationContextService;
  let queryParams: Record<string, string> = {};

  beforeEach(async () => {
    localStorage.clear();
    const addressServiceSpy = jasmine.createSpyObj('AddressService', ['buscaViaCep', 'create']);
    const cityServiceSpy = jasmine.createSpyObj('CityService', ['findAll']);
    cityServiceSpy.findAll.and.returnValue(of([]));
    queryParams = {};
    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);

    await TestBed.configureTestingModule({
      imports: [FirstAccessAddressComponent],
      providers: [
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: CityService, useValue: cityServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { get queryParamMap() { return convertToParamMap(queryParams); } } } },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']) },
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

  it('deveTornarEnderecoSalvoOAtivoEIrParaInicio', () => {
    component.onSaved({
      id: 10,
      streetName: 'Rua XV',
      number: '100',
      cityId: 1,
      cityName: 'Pato Branco',
      zipCode: '85501-000',
    });

    // HU17: o CEP vai junto, para a vitrine calcular o frete
    expect(locationContextService.current()).toEqual({
      addressId: 10,
      cityId: 1,
      cityName: 'Pato Branco',
      label: 'Rua XV, 100 · Pato Branco',
      zipCode: '85501-000',
    });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/inicio']);
  });

  // HU13
  it('deveVoltarAoReturnUrlAposSalvar', () => {
    queryParams = { returnUrl: '/resumo-pedido' };

    component.onSaved({ id: 10, streetName: 'Rua XV', number: '100', cityId: 1, cityName: 'Curitiba' } as any);

    expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/resumo-pedido');
  });
});
