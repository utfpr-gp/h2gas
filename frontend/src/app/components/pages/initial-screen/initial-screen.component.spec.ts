import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { provideNgxMask } from 'ngx-mask';

import { InitialScreenComponent } from './initial-screen.component';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';

describe('InitialScreenComponent', () => {
  let fixture: ComponentFixture<InitialScreenComponent>;
  let router: Router;

  beforeEach(async () => {
    const addressSpy = jasmine.createSpyObj('AddressService', ['buscaViaCep']);
    const citySpy = jasmine.createSpyObj('CityService', ['findAll']);
    citySpy.findAll.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [InitialScreenComponent],
      providers: [
        { provide: AddressService, useValue: addressSpy },
        { provide: CityService, useValue: citySpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        provideNgxMask(),
        provideRouter([]),
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture = TestBed.createComponent(InitialScreenComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  // HU13: porta de entrada unificada — CEP na própria landing
  it('deveExibirOPainelDeCep', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="visitor-cep"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="submit-cep"]')).toBeTruthy();
  });

  it('deveLevarAoLogin', () => {
    (fixture.nativeElement.querySelector('[data-testid="entry-login"]') as HTMLButtonElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
