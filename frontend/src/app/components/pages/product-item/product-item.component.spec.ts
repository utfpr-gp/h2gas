import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ProductItemComponent } from './product-item.component';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { PaginatedResponse } from '../../../models/paginated-response';

describe('ProductItemComponent', () => {
  let fixture: ComponentFixture<ProductItemComponent>;
  let productSpy: jasmine.SpyObj<ProductItemService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const produto = (over: Partial<ProductItem>) =>
    ({ id: 1, brand: 'Ultragaz', category: 'Gás 13kg', type: 'Gás', stockQuantity: 50, imageUrl: 'x.jpg', ...over } as ProductItem);
  const lista = [
    produto({ id: 1, price: 100 }),
    produto({ id: 2, brand: 'Liquigás', price: 95, stockQuantity: 3 }),
    produto({ id: 3, brand: 'Copagaz', stockQuantity: 0 }),
  ];
  const pagina = (data: ProductItem[], extra: Partial<PaginatedResponse<ProductItem>> = {}): PaginatedResponse<ProductItem> =>
    ({ data, page: 0, size: 10, totalElements: data.length, totalPages: 1, hasNext: false, hasPrevious: false, ...extra });

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };

  const setup = async (page = pagina(lista)) => {
    productSpy = jasmine.createSpyObj('ProductItemService', ['listMine', 'getStockSummary', 'delete']);
    productSpy.listMine.and.returnValue(of(page));
    productSpy.getStockSummary.and.returnValue(of({ total: 3, available: 2, lowStock: 1, outOfStock: 1, lowStockThreshold: 8 }));
    productSpy.delete.and.returnValue(of(void 0));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [ProductItemComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ProductItemService, useValue: productSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductItemComponent);
    fixture.detectChanges();
  };

  // HU21: a gestão mostra um preço só, sem selo de frete do produto
  // F9: o link do estoque mínimo abre a tela da loja já na seção de estoque
  it('deveLevarAoEstoqueMinimoNaSecaoCerta', async () => {
    await setup();
    const link = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="adjust-threshold"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/minha-loja/configuracoes#estoque');
  });

  it('deveExibirUmPrecoNaGestao', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const precos = Array.from(el.querySelectorAll('[data-testid="manage-price"]')) as HTMLElement[];
    expect(precos.length).toBe(3);
    expect(precos[0].textContent).toMatch(/100[.,]00/);
    expect(el.querySelector('[data-testid="manage-price-delivery"]')).toBeNull();
    expect(el.querySelector('[data-testid="manage-free-delivery"]')).toBeNull();
  });

  it('deveMostrarContadoresDoResumo', async () => {
    await setup();

    expect(byId('stock-all')?.textContent).toContain('3');
    expect(byId('stock-available')?.textContent).toContain('2');
    expect(byId('stock-low')?.textContent).toContain('1');
    expect(byId('stock-low')?.textContent).toContain('até 8');
    expect(byId('stock-out')?.textContent).toContain('1');
  });

  it('deveFiltrarPeloContador', async () => {
    await setup();
    expect(productSpy.listMine).toHaveBeenCalledWith(jasmine.objectContaining({ stock: 'all', page: 0 }));

    click('stock-low');

    expect(productSpy.listMine).toHaveBeenCalledWith(jasmine.objectContaining({ stock: 'low', page: 0 }));
    expect(byId('stock-low')?.getAttribute('aria-pressed')).toBe('true');
    expect(byId('stock-all')?.getAttribute('aria-pressed')).toBe('false');
  });

  it('deveBuscarComDebounce', fakeAsync(async () => {
    await setup();
    productSpy.listMine.calls.reset();
    const input = byId('product-search') as HTMLInputElement;

    input.value = 'liqui';
    input.dispatchEvent(new Event('input'));
    tick(100);
    expect(productSpy.listMine).not.toHaveBeenCalled();
    tick(300);

    expect(productSpy.listMine).toHaveBeenCalledOnceWith(jasmine.objectContaining({ q: 'liqui', stock: 'all', page: 0 }));
  }));

  it('deveMostrarSeloDeEstoquePorProduto', async () => {
    await setup();

    expect(byId('stock-badge-1')?.textContent).toContain('Disponível');
    expect(byId('stock-badge-2')?.textContent).toContain('Estoque baixo');
    expect(byId('stock-badge-3')?.textContent).toContain('Esgotado');
    expect(byId('product-1')?.textContent).toContain('50 un.');
  });

  it('deveConfirmarAntesDeExcluir', async () => {
    await setup();

    click('delete-1');
    expect(productSpy.delete).not.toHaveBeenCalled();
    expect(byId('delete-modal')?.textContent).toContain('Excluir Ultragaz · Gás 13kg?');

    click('confirm-delete');

    expect(productSpy.delete).toHaveBeenCalledWith(1);
    expect(toastrSpy.success).toHaveBeenCalledWith('Ultragaz · Gás 13kg foi excluído.');
    expect(productSpy.getStockSummary).toHaveBeenCalledTimes(2);
  });

  it('deveMostrarMensagemQuandoProdutoJaVendido', async () => {
    await setup();
    productSpy.delete.and.returnValue(throwError(() => ({
      status: 409, error: { message: 'Este produto já está em pedidos e não pode ser excluído. Zere o estoque para tirá-lo do catálogo.' },
    })));

    click('delete-1');
    click('confirm-delete');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Zere o estoque'));
    expect(byId('delete-modal')).toBeNull();
    expect(byId('product-1')).not.toBeNull();
  });

  it('devePaginar', async () => {
    await setup(pagina(lista, { totalElements: 25, totalPages: 3 }));

    click('next-page');

    expect(productSpy.listMine).toHaveBeenCalledWith(jasmine.objectContaining({ page: 1 }));
  });

  it('deveExibirEstadoVazioPorFiltro', async () => {
    await setup(pagina([]));

    expect(byId('empty-state')?.textContent).toContain('Nenhum produto cadastrado');
    click('stock-out');
    expect(byId('empty-state')?.textContent).toContain('Nenhum produto esgotado');
  });
});
