import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { provideNgxMask } from 'ngx-mask';

import { VisitorLocationComponent } from './visitor-location.component';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';

// HU13: /onde-voce-esta reusa o shell da porta de entrada (painel de CEP)
describe('VisitorLocationComponent', () => {
  let fixture: ComponentFixture<VisitorLocationComponent>;

  beforeEach(async () => {
    const addressSpy = jasmine.createSpyObj('AddressService', ['buscaViaCep']);
    const citySpy = jasmine.createSpyObj('CityService', ['findAll']);
    citySpy.findAll.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [VisitorLocationComponent],
      providers: [
        { provide: AddressService, useValue: addressSpy },
        { provide: CityService, useValue: citySpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        provideNgxMask(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VisitorLocationComponent);
    fixture.detectChanges();
  });

  it('deveExibirOPainelDeCep', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="visitor-cep"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="submit-cep"]')).toBeTruthy();
  });

  it('deveOferecerEntrar', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="entry-login"]')).toBeTruthy();
  });
});
