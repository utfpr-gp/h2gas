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
import { SupplierService } from '../../../services/supplier.service';
import { Storefront } from '../../../models/storefront';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';

registerLocaleData(localePt);

describe('ProductListComponent', () => {
  let component: ProductListComponent;
  let fixture: ComponentFixture<ProductListComponent>;
  let productItemServiceSpy: jasmine.SpyObj<ProductItemService>;
  let productTypeServiceSpy: jasmine.SpyObj<ProductTypeService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let supplierServiceSpy: jasmine.SpyObj<SupplierService>;

  const loja: Storefront = {
    id: 'fixture-supplier-hu02', name: 'Gás Centro Curitiba', open: true, openingTime: '07:00', closingTime: '20:00',
    averageDeliveryTime: 60, addressNeighborhood: 'Batel', cityId: 3, cityName: 'Curitiba', averageRating: 4.2, reviewCount: 14,
    bannerUrl: 'https://cdn/banner.jpg', logoUrl: 'https://cdn/logo.png', description: 'Entrega rápida no centro',
  };

  const emptyPage = { data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };

  beforeEach(async () => {
    productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['getProductsBySupplierId']);
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(of(emptyPage as any));

    productTypeServiceSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    productTypeServiceSpy.findAll.and.returnValue(of([{ id: 1, name: 'Gás' }, { id: 2, name: 'Água' }] as any));

    cartServiceSpy = jasmine.createSpyObj('CartService', ['addProduct', 'startBuyNow', 'clearAndAdd']);
    cartServiceSpy.addProduct.and.returnValue('added');
    supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getStorefront', 'getReviews']);
    supplierServiceSpy.getReviews.and.returnValue(of({ ...emptyPage, data: [{ id: 1, customerFirstName: 'Maria', rating: 5, comment: 'Ótimo', reviewedAt: null }] } as any));
    supplierServiceSpy.getStorefront.and.returnValue(of(loja));

    await TestBed.configureTestingModule({
      imports: [ProductListComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'fixture-supplier-hu02' } } } },
        { provide: ProductItemService, useValue: productItemServiceSpy },
        { provide: ProductTypeService, useValue: productTypeServiceSpy },
        { provide: ToastrService, useValue: toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: LOCALE_ID, useValue: 'pt-BR' },
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
    component.onCategorySelected({ id: 2, name: 'Água' });

    expect(productItemServiceSpy.getProductsBySupplierId)
      .toHaveBeenCalledWith('fixture-supplier-hu02', 0, 10, undefined, 'Água');
  });

  it('deveAdicionarProdutoAoCarrinhoComQuantidadeUmAoClicarEmAdicionar', () => {
    component.addToCart({ id: 5 } as ProductItem);

    expect(cartServiceSpy.addProduct).toHaveBeenCalledWith(
      jasmine.objectContaining({ id: 5, quantitySelect: 1 })
    );
    expect(toastrSpy.success).toHaveBeenCalledWith('Produto adicionado ao carrinho.');
  });

  // HU14
  it('deveAbrirDialogoAoAdicionarDeOutraLoja', () => {
    cartServiceSpy.addProduct.and.returnValue('other_supplier');

    component.addToCart({ id: 9, supplierId: 'outra' } as ProductItem);
    fixture.detectChanges();

    expect(component.pendingOtherStoreProduct).toEqual(jasmine.objectContaining({ id: 9, supplierId: 'outra' }));
    expect(fixture.nativeElement.querySelector('[data-testid="cart-other-store-dialog"]')).toBeTruthy();
    expect(toastrSpy.success).not.toHaveBeenCalled();
  });

  it('deveEsvaziarEAdicionarPeloDialogo', () => {
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
    cartServiceSpy.addProduct.and.returnValue('other_supplier');
    component.addToCart({ id: 9, supplierId: 'outra' } as ProductItem);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="keep-cart"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(cartServiceSpy.clearAndAdd).not.toHaveBeenCalled();
    expect(component.pendingOtherStoreProduct).toBeUndefined();
    expect(fixture.nativeElement.querySelector('[data-testid="cart-other-store-dialog"]')).toBeNull();
  });

  it('deveIrParaOCheckoutNaCompraDireta', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    component.buyNow({ id: 5 } as ProductItem);

    expect(cartServiceSpy.startBuyNow).toHaveBeenCalledWith(jasmine.objectContaining({ id: 5, quantitySelect: 1 }));
    expect(cartServiceSpy.addProduct).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/resumo-pedido'], { queryParams: { modo: 'compra-direta' } });
  });

  // ---- F7 §5.24 ----
  it('deveUsarFundoCinzaClaroNaTelaToda', () => {
    const host = fixture.nativeElement as HTMLElement;

    expect(host.classList).toContain('bg-gray-50');
    expect(host.classList).toContain('flex-1');
    expect(host.querySelector('body')).toBeNull();
    expect(host.querySelector('.bg-gray-50')).toBeNull();
  });

  it('deveAtenuarOGridEnquantoBusca', () => {
    const resposta = new Subject<any>();
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(resposta);

    component.onCategorySelected({ id: 1, name: 'Gás' });
    fixture.detectChanges();
    const grid = fixture.nativeElement.querySelector('[data-testid="product-grid"]') as HTMLElement;
    expect(grid.classList).toContain('opacity-60');

    resposta.next({ ...emptyPage, data: [{ id: 1, brand: 'Ultragaz' }], totalElements: 1, totalPages: 1 });
    fixture.detectChanges();
    expect(grid.classList).not.toContain('opacity-60');
  });

  // ---- F7 §5.26: cabeçalho da loja ----
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const recriarCom = (store: Storefront) => {
    supplierServiceSpy.getStorefront.and.returnValue(of(store));
    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it('deveMostrarOCabecalhoDaLoja', () => {
    expect(supplierServiceSpy.getStorefront).toHaveBeenCalledWith('fixture-supplier-hu02');
    const cabecalho = byId('store-header')!;
    expect(cabecalho.querySelector('h1')!.textContent).toContain('Gás Centro Curitiba');
    expect(cabecalho.textContent).toContain('4,2');
    expect(cabecalho.textContent).toContain('(14)');
    expect(byId('store-status')!.textContent).toContain('Aberta');
    expect(byId('store-hours')!.textContent).toContain('07:00 – 20:00');
    expect(byId('store-hours')!.textContent).toContain('60 min');
    expect(byId('store-location')!.textContent).toContain('Batel · Curitiba');
    expect((byId('store-banner-image') as HTMLImageElement).src).toBe('https://cdn/banner.jpg');
    expect((byId('store-logo') as HTMLImageElement).src).toBe('https://cdn/logo.png');
    expect(byId('store-description')!.textContent).toContain('Entrega rápida no centro');
    expect(byId('toggle-description')).toBeNull();
  });

  it('deveUsarOBannerPadraoSemBanner', () => {
    recriarCom({ ...loja, bannerUrl: null });

    expect(byId('store-banner-image')).toBeNull();
    expect(byId('store-banner-default')).not.toBeNull();
  });

  it('deveMostrarFechadaQuandoALojaNaoEstaAberta', () => {
    recriarCom({ ...loja, open: false });

    expect(byId('store-status')!.textContent).toContain('Fechada');
  });

  it('deveAlternarVerMaisNaDescricaoLonga', () => {
    recriarCom({ ...loja, description: 'Trabalhamos com gás de cozinha e industrial há mais de vinte anos, '
      + 'com entrega rápida em toda a região central e atendimento por telefone das 7h às 20h.' });
    const descricao = byId('store-description')!;
    expect(descricao.classList).toContain('line-clamp-2');

    (byId('toggle-description') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(descricao.classList).not.toContain('line-clamp-2');
    expect(byId('toggle-description')!.textContent).toContain('Ver menos');
    expect(byId('toggle-description')!.getAttribute('aria-expanded')).toBe('true');
  });

  it('deveSeguirComOsProdutosSeALojaFalhar', () => {
    supplierServiceSpy.getStorefront.and.returnValue(throwError(() => ({ error: { message: 'Loja não encontrada' } })));
    productItemServiceSpy.getProductsBySupplierId.calls.reset();
    fixture = TestBed.createComponent(ProductListComponent);
    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Loja não encontrada'));
    expect(productItemServiceSpy.getProductsBySupplierId).toHaveBeenCalled();
    expect(byId('store-header')).toBeNull();
  });

  // ---- F7 §5.27 ----
  it('deveAbrirAvaliacoesAoClicarNasEstrelas', () => {
    (fixture.nativeElement.querySelector('[data-testid="store-header"] button[data-testid="supplier-rating"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(supplierServiceSpy.getReviews).toHaveBeenCalledWith('fixture-supplier-hu02', 0, 10);
    const janela = fixture.nativeElement.querySelector('app-store-reviews-dialog [role="dialog"]') as HTMLElement;
    expect(janela.textContent).toContain('Gás Centro Curitiba');
    expect(janela.textContent).toContain('Maria');
  });

  // ---- HU13: página da loja usa a vitrine pública ----
  it('deveUsarAVitrineNoCabecalho', () => {
    expect(supplierServiceSpy.getStorefront).toHaveBeenCalledWith('fixture-supplier-hu02');
    expect(byId('store-header')!.querySelector('h1')!.textContent).toContain('Gás Centro Curitiba');
  });

  // ---- HU18: loja fechada mostra o catálogo, sem compra ----
  it('deveBloquearACompraEmLojaFechada', () => {
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(of({
      data: [{ id: 1, brand: 'Ultragaz', supplierId: 'fixture-supplier-hu02' }],
      page: 0, size: 10, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
    } as any));
    recriarCom({ ...loja, open: false, openingTime: '07:00' });

    expect(byId('store-closed-notice')!.textContent).toContain('Loja fechada · abre às 07:00');
    expect((byId('add-to-cart') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('deveLiberarACompraEmLojaAberta', () => {
    productItemServiceSpy.getProductsBySupplierId.and.returnValue(of({
      data: [{ id: 1, brand: 'Ultragaz', supplierId: 'fixture-supplier-hu02' }],
      page: 0, size: 10, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
    } as any));
    recriarCom({ ...loja, open: true });

    expect(byId('store-closed-notice')).toBeNull();
    expect((byId('add-to-cart') as HTMLButtonElement).disabled).toBeFalse();
  });
});
