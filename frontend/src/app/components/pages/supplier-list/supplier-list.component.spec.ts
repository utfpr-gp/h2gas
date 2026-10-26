import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

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
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let locationContextService: LocationContextService;
  let activatedRouteStub: { snapshot: { queryParamMap: { get: (key: string) => string | null } } };

  const emptyPage = { data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };
  const emptyProductsPage = { data: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };

  beforeEach(async () => {
    localStorage.clear();
    supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getAllSupplierOpenAndActive', 'getReviews']);
    supplierServiceSpy.getReviews.and.returnValue(of({ ...emptyPage, data: [{ id: 1, customerFirstName: 'Maria', rating: 5, comment: 'Ótimo', reviewedAt: null }] } as any));
    supplierServiceSpy.getAllSupplierOpenAndActive.and.returnValue(of(emptyPage as any));

    productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['getProductsFromActiveSuppliers']);
    productItemServiceSpy.getProductsFromActiveSuppliers.and.returnValue(of(emptyProductsPage as any));

    productTypeServiceSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    productTypeServiceSpy.findAll.and.returnValue(of([{ id: 1, name: 'Gás' }, { id: 2, name: 'Água' }] as any));

    cartServiceSpy = jasmine.createSpyObj('CartService', ['addProduct', 'startBuyNow', 'clearAndAdd']);
    cartServiceSpy.addProduct.and.returnValue('added');
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']);

    activatedRouteStub = { snapshot: { queryParamMap: { get: () => null } } };

    await TestBed.configureTestingModule({
      imports: [SupplierListComponent],
      providers: [
        provideNoopAnimations(),
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: ProductItemService, useValue: productItemServiceSpy },
        { provide: ProductTypeService, useValue: productTypeServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ToastrService, useValue: toastrSpy },
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

    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledWith(0, 10, 5, undefined, undefined);
  });

  it('deveReenviarCityIdAoBuscarProdutosGlobais', () => {
    locationContextService.setAddress({ cityId: 5, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });
    fixture.detectChanges();

    component.toggleView();

    expect(productItemServiceSpy.getProductsFromActiveSuppliers).toHaveBeenCalledWith(0, 12, 5, undefined, undefined, undefined);
  });

  it('deveFiltrarProdutosPelaCategoriaSelecionada', () => {
    fixture.detectChanges();
    component.toggleView();

    component.selectCategory({ id: 2, name: 'Gás' });

    expect(productItemServiceSpy.getProductsFromActiveSuppliers).toHaveBeenCalledWith(0, 12, undefined, 'Gás', undefined, undefined);
  });

  it('deveOrdenarPorMenorPrecoAoAtivarToggle', () => {
    fixture.detectChanges();
    component.toggleView();

    component.toggleSortByPrice();

    expect(productItemServiceSpy.getProductsFromActiveSuppliers)
      .toHaveBeenCalledWith(0, 12, undefined, undefined, 'priceDelivery,asc', undefined);
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
    expect(toastrSpy.success).toHaveBeenCalledWith('Produto adicionado ao carrinho.');
  });

  // HU14
  it('deveAbrirDialogoAoAdicionarDeOutraLoja', () => {
    fixture.detectChanges();
    cartServiceSpy.addProduct.and.returnValue('other_supplier');

    component.addToCart({ id: 9, supplierId: 'outra' } as ProductItem);
    fixture.detectChanges();

    expect(component.pendingOtherStoreProduct).toEqual(jasmine.objectContaining({ id: 9, supplierId: 'outra' }));
    expect(fixture.nativeElement.querySelector('[data-testid="cart-other-store-dialog"]')).toBeTruthy();
    expect(toastrSpy.success).not.toHaveBeenCalled();
  });

  it('deveEsvaziarEAdicionarPeloDialogo', () => {
    fixture.detectChanges();
    cartServiceSpy.addProduct.and.returnValue('other_supplier');
    component.addToCart({ id: 9, supplierId: 'outra' } as ProductItem);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="clear-and-add"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(cartServiceSpy.clearAndAdd).toHaveBeenCalledWith(jasmine.objectContaining({ id: 9, supplierId: 'outra' }));
    expect(component.pendingOtherStoreProduct).toBeUndefined();
    expect(toastrSpy.success).toHaveBeenCalledWith('Produto adicionado ao carrinho.');
  });

  it('deveManterCarrinhoAoCancelar', () => {
    fixture.detectChanges();
    cartServiceSpy.addProduct.and.returnValue('other_supplier');
    component.addToCart({ id: 9, supplierId: 'outra' } as ProductItem);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="keep-cart"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(cartServiceSpy.clearAndAdd).not.toHaveBeenCalled();
    expect(component.pendingOtherStoreProduct).toBeUndefined();
    expect(fixture.nativeElement.querySelector('[data-testid="cart-other-store-dialog"]')).toBeNull();
  });

  // ---- F7 §5.21 ----
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const digitar = (valor: string) => {
    const input = byId('catalog-search') as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  it('deveBuscarLojasDaCidadeAoDigitar', fakeAsync(() => {
    locationContextService.setAddress({ cityId: 5, cityName: 'Curitiba', label: 'Rua XV · Curitiba' });
    fixture.detectChanges();
    supplierServiceSpy.getAllSupplierOpenAndActive.calls.reset();

    digitar('gás');
    tick(100);
    expect(supplierServiceSpy.getAllSupplierOpenAndActive).not.toHaveBeenCalled();
    tick(300);

    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledOnceWith(0, 10, 5, 'gás', undefined);
    expect(byId('filter-button')).toBeNull();
  }));

  it('deveBuscarProdutosNaVisaoProdutos', fakeAsync(() => {
    locationContextService.setAddress({ cityId: 5, cityName: 'Curitiba', label: 'Rua XV · Curitiba' });
    fixture.detectChanges();
    byId('view-products')!.click();
    fixture.detectChanges();
    productItemServiceSpy.getProductsFromActiveSuppliers.calls.reset();

    digitar('minalba');
    tick(300);

    expect(productItemServiceSpy.getProductsFromActiveSuppliers).toHaveBeenCalledOnceWith(0, 12, 5, undefined, undefined, 'minalba');
    expect(supplierServiceSpy.getAllSupplierOpenAndActive).not.toHaveBeenCalledWith(jasmine.anything(), jasmine.anything(), jasmine.anything(), 'minalba');
  }));

  it('deveLimparABusca', fakeAsync(() => {
    fixture.detectChanges();
    digitar('gás');
    tick(300);
    fixture.detectChanges();
    supplierServiceSpy.getAllSupplierOpenAndActive.calls.reset();

    byId('clear-search')!.click();
    fixture.detectChanges();

    expect((byId('catalog-search') as HTMLInputElement).value).toBe('');
    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledWith(0, 10, undefined, undefined, undefined);
    expect(byId('clear-search')).toBeNull();
  }));

  it('devePaginarProdutos', () => {
    productItemServiceSpy.getProductsFromActiveSuppliers.and.returnValue(of({ ...emptyProductsPage, data: [{ id: 1 }] as any,
      size: 12, totalElements: 30, totalPages: 3 } as any));
    fixture.detectChanges();
    component.toggleView();
    fixture.detectChanges();

    component.onPageChange(2);

    expect(productItemServiceSpy.getProductsFromActiveSuppliers).toHaveBeenCalledWith(2, 12, undefined, undefined, undefined, undefined);
    expect(fixture.nativeElement.textContent).toContain('30 produtos');
  });

  it('deveAlternarEntreLojasEProdutos', () => {
    fixture.detectChanges();
    expect(byId('view-suppliers')!.getAttribute('aria-pressed')).toBe('true');

    byId('view-products')!.click();
    fixture.detectChanges();
    expect(component.viewMode).toBe('products');
    expect(byId('view-products')!.getAttribute('aria-pressed')).toBe('true');
    // controle segmentado claro: o ativo não fica com fundo escuro
    expect(byId('view-products')!.classList).not.toContain('bg-primary');

    byId('view-suppliers')!.click();
    fixture.detectChanges();
    expect(component.viewMode).toBe('suppliers');
  });

  it('deveManterAHierarquiaDeTitulosNaVisaoProdutos', () => {
    fixture.detectChanges();
    byId('view-products')!.click();
    fixture.detectChanges();

    // h1 → h2 → h3 dos cards: sem pular nível
    expect(fixture.nativeElement.querySelectorAll('h1').length).toBe(1);
    expect(fixture.nativeElement.querySelector('h2')!.textContent).toContain('Produtos disponíveis');
  });

  it('deveUsarTextoVerdadeiroNosBanners', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('15% OFF');
  });

  it('deveTerAlvosDeToqueDe44pxNaBuscaEAlternancia', () => {
    fixture.detectChanges();

    expect(byId('view-suppliers')!.classList).toContain('min-h-11');
    expect(byId('view-products')!.classList).toContain('min-h-11');
    byId('view-products')!.click();
    fixture.detectChanges();
    expect(byId('sort-price')!.classList).toContain('min-h-11');
  });

  it('deveIrParaOCheckoutNaCompraDireta', () => {
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    component.buyNow({ id: 99, brand: 'Ultragaz' } as ProductItem);

    expect(cartServiceSpy.startBuyNow).toHaveBeenCalledWith(jasmine.objectContaining({ id: 99, quantitySelect: 1 }));
    expect(cartServiceSpy.addProduct).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/resumo-pedido'], { queryParams: { modo: 'compra-direta' } });
  });

  // ---- F7 §5.27 ----
  it('deveAbrirAvaliacoesAoClicarNasEstrelas', () => {
    component.openReviews({ id: 'loja-9', name: 'Gás Centro', averageRating: 4.2, reviewCount: 14 } as any);
    fixture.detectChanges();

    expect(supplierServiceSpy.getReviews).toHaveBeenCalledWith('loja-9', 0, 10);
    const janela = fixture.nativeElement.querySelector('app-store-reviews-dialog [role="dialog"]') as HTMLElement;
    expect(janela.textContent).toContain('Gás Centro');
    expect(janela.textContent).toContain('Maria');

    (fixture.nativeElement.querySelector('[data-testid="close-reviews"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-store-reviews-dialog')).toBeNull();
  });

  // ---- HU15 ----

  it('deveExibirAsLojasEmGrade', () => {
    supplierServiceSpy.getAllSupplierOpenAndActive.and.returnValue(of({
      data: [{ id: '1', name: 'Gás Centro' }, { id: '2', name: 'Água Boa' }],
      page: 0, size: 10, totalElements: 2, totalPages: 1, hasNext: false, hasPrevious: false
    }));
    fixture.detectChanges();

    const grade = fixture.nativeElement.querySelector('[data-testid="supplier-grid"]') as HTMLElement;
    expect(grade).not.toBeNull();
    expect(grade.classList).toContain('grid');
    expect(grade.querySelectorAll('app-supplier-card').length).toBe(2);
  });

  it('deveMostrarOsChipsNaVisaoDeLojas', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-category-chip-bar')).not.toBeNull();
    expect(productTypeServiceSpy.findAll).toHaveBeenCalled();
  });

  it('deveEnviarOProductTypeIdAoFiltrarLojas', () => {
    fixture.detectChanges();

    component.selectCategory({ id: 3, name: 'Gás' });

    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledWith(0, 10, undefined, undefined, 3);
  });

  it('deveVoltarParaAPrimeiraPaginaAoTrocarDeTipo', () => {
    fixture.detectChanges();
    component.onPageChange(2);
    supplierServiceSpy.getAllSupplierOpenAndActive.calls.reset();

    component.selectCategory({ id: 3, name: 'Gás' });

    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledWith(0, 10, undefined, undefined, 3);
  });

  it('deveLimparOFiltroDeTipoEmTodas', () => {
    fixture.detectChanges();
    component.selectCategory({ id: 3, name: 'Gás' });
    supplierServiceSpy.getAllSupplierOpenAndActive.calls.reset();

    component.selectCategory(null);

    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledWith(0, 10, undefined, undefined, undefined);
  });

  it('deveMostrarEstadoVazioQuandoNaoHaLojaDoTipo', () => {
    supplierServiceSpy.getAllSupplierOpenAndActive.and.returnValue(of({
      data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false
    }));
    fixture.detectChanges();

    component.selectCategory({ id: 3, name: 'Gás' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="empty-state"]')).not.toBeNull();
  });
});
