import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CategoryProductService } from './category-product.service';
import { environment } from '../../environments/environment';

describe('CategoryProductService', () => {
  let service: CategoryProductService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CategoryProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('deveListarMinhasCategorias', () => {
    service.listMine().subscribe((r) => expect(r[0].productCount).toBe(3));

    const req = httpMock.expectOne(`${environment.apiUrl}/category-products/mine`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 5, name: 'P13', type: 'Gás', productTypeId: 2, typeIcon: 'local_fire_department', productCount: 3 }]);
  });

  it('deveCriarSoComNomeETipo', () => {
    service.createCategory({ name: 'P45', productTypeId: 2 }).subscribe();
    const post = httpMock.expectOne(`${environment.apiUrl}/category-products`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'P45', productTypeId: 2 });
    post.flush({ id: 9, name: 'P45' });

    service.updateCategory(9, { name: 'P45 Plus', productTypeId: 2 }).subscribe();
    const put = httpMock.expectOne(`${environment.apiUrl}/category-products/9`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ name: 'P45 Plus', productTypeId: 2 });
    put.flush({ id: 9, name: 'P45 Plus' });
  });
});
