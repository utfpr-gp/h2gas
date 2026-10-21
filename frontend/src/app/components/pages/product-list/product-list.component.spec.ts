import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of, Subject, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

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
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const emptyPage = { data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };

  beforeEach(async () => {
    productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['getProductsBySupplierId']);
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(of(emptyPage as any));

    productTypeServiceSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    productTypeServiceSpy.findAll.and.returnValue(of([{ id: 1, name: 'Gás' }, { id: 2, name: 'Água' }] as any));

    cartServiceSpy = jasmine.createSpyObj('CartService', ['addProduct', 'startBuyNow']);

    await TestBed.configureTestingModule({
      imports: [ProductListComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'fixture-supplier-hu02' } } } },
        { provide: ProductItemService, useValue: productItemServiceSpy },
        { provide: ProductTypeService, useValue: productTypeServiceSpy },
        { provide: ToastrService, useValue: toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
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

  // F7 §5.10: carregar o catálogo não gera toast
  it('naoDeveExibirToastDeSucessoAoBuscar', () => {
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(of({ ...emptyPage, data: [{ id: 1, brand: 'Ultragaz' }], totalElements: 1, totalPages: 1 } as any));

    component.ngOnInit();

    expect(productItemServiceSpy.getProductsBySupplierId).toHaveBeenCalled();
    expect(toastrSpy.success).not.toHaveBeenCalled();
  });

  it('deveMostrarCausaAoFalharCarregarCategorias', () => {
    productTypeServiceSpy.findAll.and.returnValue(throwError(() => ({ status: 0, error: new ProgressEvent('error') })));

    component.loadCategories();

    expect(toastrSpy.error).toHaveBeenCalledWith('Erro ao carregar as categorias: sem conexão com o servidor');
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

  it('deveIrParaOCheckoutNaCompraDireta', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    component.buyNow({ id: 5 } as ProductItem);

    expect(cartServiceSpy.startBuyNow).toHaveBeenCalledWith(jasmine.objectContaining({ id: 5, quantitySelect: 1 }));
    expect(cartServiceSpy.addProduct).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/resumo-pedido'], { queryParams: { modo: 'compra-direta' } });
  });

  // ---- F7 §5.24 ----
  it('deveManterSoUmFundoNaTelaDeProdutos', () => {
    const html = fixture.nativeElement as HTMLElement;

    expect(html.querySelector('body')).toBeNull();
    expect(html.querySelector('.bg-gray-50')).toBeNull();
  });

  it('deveAtenuarOGridEnquantoBusca', () => {
    const resposta = new Subject<any>();
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(resposta);

    component.onCategorySelected('Gás');
    fixture.detectChanges();
    const grid = fixture.nativeElement.querySelector('[data-testid="product-grid"]') as HTMLElement;
    expect(grid.classList).toContain('opacity-60');

    resposta.next({ ...emptyPage, data: [{ id: 1, brand: 'Ultragaz' }], totalElements: 1, totalPages: 1 });
    fixture.detectChanges();
    expect(grid.classList).not.toContain('opacity-60');
  });
});
