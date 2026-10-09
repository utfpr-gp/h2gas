import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of } from 'rxjs';

import { ProductListComponent } from './product-list.component';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { CartService } from '../../../services/cart.service';
import { ProductItem } from '../../../models/product-item';

describe('ProductListComponent', () => {
  let component: ProductListComponent;
  let fixture: ComponentFixture<ProductListComponent>;
  let productItemServiceSpy: jasmine.SpyObj<ProductItemService>;
  let productTypeServiceSpy: jasmine.SpyObj<ProductTypeService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;

  const emptyPage = { data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };

  beforeEach(async () => {
    productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['getProductsBySupplierId']);
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(of(emptyPage as any));

    productTypeServiceSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    productTypeServiceSpy.findAll.and.returnValue(of([{ id: 1, name: 'Gás' }, { id: 2, name: 'Água' }] as any));

    cartServiceSpy = jasmine.createSpyObj('CartService', ['addProduct']);

    await TestBed.configureTestingModule({
      imports: [ProductListComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'fixture-supplier-hu02' } } } },
        { provide: ProductItemService, useValue: productItemServiceSpy },
        { provide: ProductTypeService, useValue: productTypeServiceSpy },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
        { provide: CartService, useValue: cartServiceSpy },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveFiltrarAoSelecionarCategoriaPeloChip', () => {
    component.onCategorySelected('Água');

    expect(productItemServiceSpy.getProductsBySupplierId)
      .toHaveBeenCalledWith('fixture-supplier-hu02', 0, 10, undefined, 'Água');
  });

  it('deveAdicionarProdutoAoCarrinhoComQuantidadeUmAoClicarEmAdicionar', () => {
    component.addToCart({ id: 5 } as ProductItem);

    expect(cartServiceSpy.addProduct).toHaveBeenCalledWith(
      jasmine.objectContaining({ id: 5, quantitySelect: 1 })
    );
  });
});
