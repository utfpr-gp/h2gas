import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ProductTypeService } from './product-type.service';
import { environment } from '../../environments/environment';

describe('ProductTypeService', () => {
  let service: ProductTypeService;
  let http: HttpTestingController;
  const url = `${environment.apiUrl}/product-types`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProductTypeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deveCriarTipo', () => {
    service.create({ name: 'Gás' }).subscribe((t) => expect(t.id).toBe(1));

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Gás' });
    req.flush({ id: 1, name: 'Gás' });
  });

  it('deveRenomearTipo', () => {
    service.update(1, { name: 'Gás GLP' }).subscribe((t) => expect(t.name).toBe('Gás GLP'));

    const req = http.expectOne(`${url}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ name: 'Gás GLP' });
    req.flush({ id: 1, name: 'Gás GLP' });
  });
});
