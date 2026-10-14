import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AddressService } from './address.service';
import { environment } from '../../environments/environment';

describe('AddressService', () => {
  let service: AddressService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AddressService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('deveBuscarTodosEnderecosDoUsuario', () => {
    let resultado: any[] = [];
    service.findAllByUser('user-1').subscribe((r) => (resultado = r));

    const req = httpMock.expectOne(`${environment.apiUrl}/addresses/by-user/user-1/all`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, addressName: 'Casa' }, { id: 2, addressName: 'Trabalho' }]);

    expect(resultado.length).toBe(2);
  });
});
