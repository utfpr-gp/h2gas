import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { ManageAddressesComponent } from './manage-addresses.component';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';
import { UserService } from '../../../services/user.service';
import { LocationContextService } from '../../../services/location-context.service';
import { Address } from '../../../models/address';

describe('ManageAddressesComponent', () => {
  let component: ManageAddressesComponent;
  let fixture: ComponentFixture<ManageAddressesComponent>;
  let addressServiceSpy: jasmine.SpyObj<AddressService>;
  let locationContextService: LocationContextService;

  const casa: Address = { id: 1, addressName: 'Casa', streetName: 'Rua Um', number: '10', cityId: 1, cityName: 'Pato Branco' };
  const trabalho: Address = { id: 2, addressName: 'Trabalho', streetName: 'Rua Dois', number: '20', cityId: 1, cityName: 'Pato Branco' };

  beforeEach(async () => {
    localStorage.clear();
    addressServiceSpy = jasmine.createSpyObj('AddressService', ['findAllByUser', 'update', 'buscaViaCep', 'create']);
    addressServiceSpy.findAllByUser.and.returnValue(of([casa, trabalho]));
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'user-1' });
    const cityServiceSpy = jasmine.createSpyObj('CityService', ['findAll']);
    cityServiceSpy.findAll.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ManageAddressesComponent],
      providers: [
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: CityService, useValue: cityServiceSpy },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']) },
        provideNgxMask(),
      ],
    }).compileComponents();

    locationContextService = TestBed.inject(LocationContextService);
    fixture = TestBed.createComponent(ManageAddressesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveListarEnderecosJaCadastrados', () => {
    expect(addressServiceSpy.findAllByUser).toHaveBeenCalledWith('user-1');
    expect(component.addresses.length).toBe(2);

    const texto = (fixture.nativeElement as HTMLElement).textContent;
    expect(texto).toContain('Casa');
    expect(texto).toContain('Trabalho');
  });

  it('deveAdicionarNovoEnderecoComRotuloIdentificadorSemAfetarExistentes', () => {
    const novo: Address = { id: 3, addressName: 'Academia', streetName: 'Rua Três', number: '30', cityId: 1, cityName: 'Pato Branco' };
    addressServiceSpy.findAllByUser.and.returnValue(of([novo, casa, trabalho]));
    component.showForm = true;

    component.onAddressSaved(novo);

    expect(addressServiceSpy.update).not.toHaveBeenCalled();
    expect(addressServiceSpy.findAllByUser).toHaveBeenCalledTimes(2);
    expect(component.addresses.length).toBe(3);
    expect(component.showForm).toBeFalse();
  });

  it('deveTornarEnderecoAtivoAoSelecionar', () => {
    component.select(trabalho);

    expect(locationContextService.current()?.addressId).toBe(2);
    expect(component.isActive(trabalho)).toBeTrue();
    expect(component.isActive(casa)).toBeFalse();
  });
});
