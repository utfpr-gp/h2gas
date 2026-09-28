import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';
import { LocationContextService } from './location-context.service';

describe('AuthService', () => {
  let service: AuthService;
  let locationContextService: LocationContextService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient()],
    });
    service = TestBed.inject(AuthService);
    locationContextService = TestBed.inject(LocationContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('deveLimparEnderecoAtivoAoFazerLogout', () => {
    localStorage.setItem('access_token', 'fake-token');
    localStorage.setItem('user', JSON.stringify({ id: 'user-1' }));
    locationContextService.setAddress({ cityId: 1, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });

    service.logout();

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(locationContextService.current()).toBeNull();
    expect(localStorage.getItem('h2gas.activeAddress')).toBeNull();
  });
});
