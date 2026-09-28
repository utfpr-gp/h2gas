import { TestBed } from '@angular/core/testing';

import { LocationContextService } from './location-context.service';

describe('LocationContextService', () => {
  let service: LocationContextService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(LocationContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('devePersistirEnderecoAtivoNoLocalStorage', () => {
    service.setAddress({ cityId: 1, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });

    expect(service.current()).toEqual({ cityId: 1, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });
    expect(JSON.parse(localStorage.getItem('h2gas.activeAddress')!)).toEqual({
      cityId: 1,
      cityName: 'Pato Branco',
      label: 'Rua XV, 100 · Pato Branco',
    });
  });

  it('deveRecuperarEnderecoAtivoAposReload', () => {
    localStorage.setItem(
      'h2gas.activeAddress',
      JSON.stringify({ cityId: 2, cityName: 'Guarapuava', label: 'Rua XV, 7951 · Guarapuava' })
    );

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const reloadedService = TestBed.inject(LocationContextService);

    expect(reloadedService.current()).toEqual({
      cityId: 2,
      cityName: 'Guarapuava',
      label: 'Rua XV, 7951 · Guarapuava',
    });
  });
});
