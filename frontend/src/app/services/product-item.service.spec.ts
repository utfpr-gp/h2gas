import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { ProductItemService } from './product-item.service';
import { environment } from '../../environments/environment';

describe('ProductItemService', () => {
  let service: ProductItemService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductItemService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('deveMontarQueryStringComCategoriaESort', () => {
    service.getProductsFromActiveSuppliers(0, 20, 5, 'Gás', 'priceDelivery,asc').subscribe();

    const req = httpMock.expectOne(
      `${environment.apiUrl}/product-items/by-supplier-open?page=0&size=20&cityId=5&category=G%C3%A1s&sort=priceDelivery,asc`
    );
    expect(req.request.method).toBe('GET');
    req.flush({ data: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  });

  it('deveMontarQueryStringSemFiltrosOpcionaisQuandoAusentes', () => {
    service.getProductsFromActiveSuppliers().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/product-items/by-supplier-open?page=0&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush({ data: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  });
});
