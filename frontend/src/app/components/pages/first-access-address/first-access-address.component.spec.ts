import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
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

  beforeEach(async () => {
    localStorage.clear();
    const addressServiceSpy = jasmine.createSpyObj('AddressService', ['buscaViaCep', 'create']);
    const cityServiceSpy = jasmine.createSpyObj('CityService', ['findAll']);
    cityServiceSpy.findAll.and.returnValue(of([]));
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [FirstAccessAddressComponent],
      providers: [
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: CityService, useValue: cityServiceSpy },
        { provide: Router, useValue: routerSpy },
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
    });

    expect(locationContextService.current()).toEqual({
      addressId: 10,
      cityId: 1,
      cityName: 'Pato Branco',
      label: 'Rua XV, 100 · Pato Branco',
    });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/inicio']);
  });
});
