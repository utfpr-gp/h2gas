import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { AddressFormComponent } from './address-form.component';
import { AddressService } from '../../services/address.service';
import { CityService } from '../../services/city.service';

describe('AddressFormComponent', () => {
  let component: AddressFormComponent;
  let fixture: ComponentFixture<AddressFormComponent>;
  let addressServiceSpy: jasmine.SpyObj<AddressService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const patoBranco = { id: 1, name: 'Pato Branco', state: 'PR' };

  beforeEach(async () => {
    addressServiceSpy = jasmine.createSpyObj('AddressService', ['buscaViaCep', 'create']);
    const cityServiceSpy = jasmine.createSpyObj('CityService', ['findAll']);
    cityServiceSpy.findAll.and.returnValue(of([patoBranco] as any));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [AddressFormComponent],
      providers: [
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: CityService, useValue: cityServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideNgxMask(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddressFormComponent);
    component = fixture.componentInstance;
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

  it('deveExibirErroParaCepInexistente', () => {
    addressServiceSpy.buscaViaCep.and.returnValue(of({ erro: true }));

    component.form.get('zipCode')?.setValue('80000-001');

    expect(() => component.buscarCep()).not.toThrow();
    expect(component.cidadeEncontrada).toBeFalse();
    expect(component.isLoadingCep).toBeFalse();
    expect(toastrSpy.error).toHaveBeenCalledWith('CEP não encontrado. Confira e tente novamente.');
  });

  it('deveEmitirEnderecoSalvoComRotulo', () => {
    addressServiceSpy.create.and.returnValue(of({ id: 10 } as any));
    spyOn(component.saved, 'emit');

    component.form.setValue({
      addressName: 'Trabalho',
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
      jasmine.objectContaining({ addressName: 'Trabalho', cityId: 1 })
    );
    expect(component.saved.emit).toHaveBeenCalledWith(
      jasmine.objectContaining({
        id: 10,
        addressName: 'Trabalho',
        streetName: 'Rua XV',
        number: '100',
        cityId: 1,
        cityName: 'Pato Branco',
      })
    );
  });
});
