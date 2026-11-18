import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideNgxMask } from 'ngx-mask';

import { VisitorCepPanelComponent } from './visitor-cep-panel.component';
import { AddressService } from '../../services/address.service';
import { CityService } from '../../services/city.service';
import { LocationContextService } from '../../services/location-context.service';

// HU13: o visitante escolhe a cidade pelo CEP para ver o catálogo (RN05)
describe('VisitorCepPanelComponent', () => {
  let fixture: ComponentFixture<VisitorCepPanelComponent>;
  let component: VisitorCepPanelComponent;
  let addressSpy: jasmine.SpyObj<AddressService>;
  let routerSpy: { navigateByUrl: jasmine.Spy };
  let location: LocationContextService;
  let queryParams: Record<string, string> = {};
  const curitiba = { id: 3, name: 'Curitiba', state: 'PR', ibgeCode: 4106902 };
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;

  const informarCep = (cep: string) => {
    component.form.setValue({ cep });
    (byId('submit-cep') as HTMLButtonElement).click();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    localStorage.clear();
    queryParams = {};
    addressSpy = jasmine.createSpyObj('AddressService', ['buscaViaCep']);
    const citySpy = jasmine.createSpyObj('CityService', ['findAll']);
    citySpy.findAll.and.returnValue(of([curitiba]));
    await TestBed.configureTestingModule({
      imports: [VisitorCepPanelComponent],
      providers: [
        { provide: AddressService, useValue: addressSpy },
        { provide: CityService, useValue: citySpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { get queryParamMap() { return convertToParamMap(queryParams); } } } },
        provideNgxMask(),
      ],
    }).compileComponents();
    routerSpy = { navigateByUrl: spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true) };
    location = TestBed.inject(LocationContextService);
    fixture = TestBed.createComponent(VisitorCepPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deveDefinirACidadePeloCep', () => {
    addressSpy.buscaViaCep.and.returnValue(of({ localidade: 'Curitiba', uf: 'PR', ibge: '4106902', bairro: 'Batel' }));

    informarCep('80420-000');

    expect(addressSpy.buscaViaCep).toHaveBeenCalledWith('80420000');
    // HU17: o CEP fica guardado para a vitrine e o carrinho calcularem a distância e o frete
    expect(location.current()).toEqual(jasmine.objectContaining({ cityId: 3, cityName: 'Curitiba', label: 'Batel · Curitiba', zipCode: '80420000' }));
    expect(location.current()!.addressId).toBeUndefined();
    expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/inicio');
  });

  it('deveAvisarCidadeNaoAtendida', () => {
    addressSpy.buscaViaCep.and.returnValue(of({ localidade: 'Londrina', uf: 'PR', ibge: '4113700', bairro: 'Centro' }));

    informarCep('86010-000');

    expect(byId('cep-message')!.textContent).toContain('Ainda não atendemos Londrina/PR');
    expect(location.current()).toBeNull();
    expect(routerSpy.navigateByUrl).not.toHaveBeenCalled();
  });

  it('deveAvisarCepInvalidoOuInexistente', () => {
    informarCep('8042');
    expect(byId('cep-message')!.textContent).toContain('CEP inválido');
    expect(addressSpy.buscaViaCep).not.toHaveBeenCalled();

    addressSpy.buscaViaCep.and.returnValue(of({ erro: true }));
    informarCep('99999-999');
    expect(byId('cep-message')!.textContent).toContain('CEP não encontrado');
  });

  it('deveAvisarFalhaDoViaCep', () => {
    addressSpy.buscaViaCep.and.returnValue(throwError(() => ({ status: 0 })));

    informarCep('80420-000');

    expect(byId('cep-message')!.textContent).toContain('Não foi possível consultar o CEP agora');
  });

  it('deveVoltarParaOReturnUrl', () => {
    queryParams = { returnUrl: '/lojas/loja-1' };
    addressSpy.buscaViaCep.and.returnValue(of({ localidade: 'Curitiba', uf: 'PR', ibge: '4106902', bairro: '' }));

    informarCep('80420-000');

    expect(location.current()!.label).toBe('Curitiba');
    expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/lojas/loja-1');
  });
});
