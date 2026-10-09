import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of } from 'rxjs';

import { SupplierListComponent } from './supplier-list.component';
import { SupplierService } from '../../../services/supplier.service';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { LocationContextService } from '../../../services/location-context.service';
import { CartService } from '../../../services/cart.service';
import { ProductItem } from '../../../models/product-item';

describe('SupplierListComponent', () => {
  let component: SupplierListComponent;
  let fixture: ComponentFixture<SupplierListComponent>;
  let supplierServiceSpy: jasmine.SpyObj<SupplierService>;
  let productItemServiceSpy: jasmine.SpyObj<ProductItemService>;
  let productTypeServiceSpy: jasmine.SpyObj<ProductTypeService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let locationContextService: LocationContextService;
  let activatedRouteStub: { snapshot: { queryParamMap: { get: (key: string) => string | null } } };

  const emptyPage = { data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };
  const emptyProductsPage = { data: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };

  beforeEach(async () => {
    localStorage.clear();
    supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getAllSupplierOpenAndActive', 'searchSuppliers']);
    supplierServiceSpy.getAllSupplierOpenAndActive.and.returnValue(of(emptyPage as any));

    productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['getProductsFromActiveSuppliers']);
    productItemServiceSpy.getProductsFromActiveSuppliers.and.returnValue(of(emptyProductsPage as any));

    productTypeServiceSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    productTypeServiceSpy.findAll.and.returnValue(of([{ id: 1, name: 'Gás' }, { id: 2, name: 'Água' }] as any));

    cartServiceSpy = jasmine.createSpyObj('CartService', ['addProduct']);

    activatedRouteStub = { snapshot: { queryParamMap: { get: () => null } } };

    await TestBed.configureTestingModule({
      imports: [SupplierListComponent],
      providers: [
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: ProductItemService, useValue: productItemServiceSpy },
        { provide: ProductTypeService, useValue: productTypeServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
      ]
    })
    .compileComponents();

    locationContextService = TestBed.inject(LocationContextService);
    fixture = TestBed.createComponent(SupplierListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('deveEnviarCityIdDoContextoAoBuscarFornecedores', () => {
    locationContextService.setAddress({ cityId: 5, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });

    fixture.detectChanges();

    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledWith(0, 10, 5);
  });

  it('deveReenviarCityIdAoBuscarProdutosGlobais', () => {
    locationContextService.setAddress({ cityId: 5, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });
    fixture.detectChanges();

    component.toggleView();

    expect(productItemServiceSpy.getProductsFromActiveSuppliers).toHaveBeenCalledWith(0, 20, 5, undefined, undefined);
  });

  it('deveFiltrarProdutosPelaCategoriaSelecionada', () => {
    fixture.detectChanges();
    component.toggleView();

    component.selectCategory('Gás');

    expect(productItemServiceSpy.getProductsFromActiveSuppliers).toHaveBeenCalledWith(0, 20, undefined, 'Gás', undefined);
  });

  it('deveOrdenarPorMenorPrecoAoAtivarToggle', () => {
    fixture.detectChanges();
    component.toggleView();

    component.toggleSortByPrice();

    expect(productItemServiceSpy.getProductsFromActiveSuppliers)
      .toHaveBeenCalledWith(0, 20, undefined, undefined, 'priceDelivery,asc');
  });

  it('deveIniciarNaVisaoDeProdutosQuandoQueryParamViewForProducts', () => {
    activatedRouteStub.snapshot.queryParamMap.get = (key: string) => (key === 'view' ? 'products' : null);

    fixture.detectChanges();

    expect(component.viewMode).toBe('products');
    expect(productItemServiceSpy.getProductsFromActiveSuppliers).toHaveBeenCalled();
  });

  it('deveAdicionarProdutoAoCarrinhoComQuantidadeUmAoClicarEmAdicionar', () => {
    fixture.detectChanges();
    const produto = { id: 99, brand: 'Ultragaz' } as ProductItem;

    component.addToCart(produto);

    expect(cartServiceSpy.addProduct).toHaveBeenCalledWith(
      jasmine.objectContaining({ id: 99, quantitySelect: 1 })
    );
  });
});
