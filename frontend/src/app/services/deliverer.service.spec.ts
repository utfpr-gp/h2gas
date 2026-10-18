import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DelivererService } from './deliverer.service';
import { environment } from '../../environments/environment';

describe('DelivererService', () => {
  let service: DelivererService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DelivererService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deveListarFrotaDoFornecedor', () => {
    service.listMine().subscribe((frota) => expect(frota[0].busy).toBeTrue());

    const req = httpMock.expectOne(`${environment.apiUrl}/deliverers/mine`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 7, name: 'Carlos', busy: true }]);
  });

  it('deveCadastrarEntregador', () => {
    const novo = { name: 'Carlos', email: 'c@h2gas.local', phoneNumber: '41988887777', password: 'senha123', vehicleInfo: 'Moto' };
    service.create(novo).subscribe((criado) => expect(criado.id).toBe(7));

    const req = httpMock.expectOne(`${environment.apiUrl}/deliverers`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(novo);
    req.flush({ id: 7, name: 'Carlos' });
  });
});
