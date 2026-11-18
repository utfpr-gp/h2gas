import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { OrderHistoryComponent } from './order-history.component';
import { OrderService } from '../../../services/order.service';
import { SupplierService } from '../../../services/supplier.service';
import { UserService } from '../../../services/user.service';
import { OrderResponse } from '../../../models/order.model';
import { PaginatedResponse } from '../../../models/paginated-response';

describe('OrderHistoryComponent', () => {
  let fixture: ComponentFixture<OrderHistoryComponent>;
  let orderSpy: jasmine.SpyObj<OrderService>;

  const entregue = {
    id: 'a1b2c3d4-0000-0000-0000-000000000001', createdAt: '2026-10-05T10:00:00' as any, totalPrice: 120, paymentMethod: 'PIX',
    customerName: 'Maria Souza', status: 'Entregue', deliveryMode: 'DELIVERY', streetName: 'Rua XV', number: '700',
    neighborhood: 'Centro', cityName: 'Curitiba', delivererName: 'Carlos', deliveredAt: '2026-10-05T10:40:00',
    orderItems: [{ id: 1, productId: 4, productBrand: 'Ultragaz', productNameCategory: 'Gás 13kg', quantity: 1, unitPrice: 120, totalPrice: 120 }],
  } as unknown as OrderResponse;
  const cancelado = {
    id: 'ffee0000-0000-0000-0000-000000000002', createdAt: '2026-10-04T09:00:00' as any, totalPrice: 80, paymentMethod: 'DINHEIRO',
    customerName: 'João Lima', status: 'Cancelado', deliveryMode: 'PICKUP', cancellationReason: 'Sem estoque do P13', orderItems: [],
  } as unknown as OrderResponse;

  const pagina = (data: OrderResponse[], extra: Partial<PaginatedResponse<OrderResponse>> = {}): PaginatedResponse<OrderResponse> =>
    ({ data, page: 0, size: 10, totalElements: data.length, totalPages: 1, hasNext: false, hasPrevious: false, ...extra });
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const escolher = (id: string, valor: string) => {
    const el = byId(id) as HTMLSelectElement;
    el.value = valor;
    el.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const ultimoFiltro = () => orderSpy.listMyHistory.calls.mostRecent().args[0];

  // "hoje" fixo em 21/10/2026 para os períodos; o teste do debounce usa o fakeAsync e dispensa o relógio
  let clockInstalled = false;
  const setup = async (page = pagina([entregue, cancelado]), mockToday = true) => {
    if (mockToday) {
      jasmine.clock().install();
      jasmine.clock().mockDate(new Date(2026, 9, 21, 12, 0, 0));
      clockInstalled = true;
    }
    orderSpy = jasmine.createSpyObj('OrderService', ['listMyHistory', 'getMyHistorySummary']);
    orderSpy.listMyHistory.and.returnValue(of(page));
    orderSpy.getMyHistorySummary.and.returnValue(of({ delivered: 70, canceled: 4, revenue: 4523, averageTicket: 64.61 }));
    const supplierSpy = jasmine.createSpyObj('SupplierService', ['getOpen', 'getMySettings']);
    supplierSpy.getOpen.and.returnValue(of(true));
    supplierSpy.getMySettings.and.returnValue(of({ openingTime: '08:00', closingTime: '18:00', averageDeliveryTime: 30, lowStockThreshold: 8 }));
    const userSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userSpy.getUserLocalStorage.and.returnValue({ id: 'f1', supplierId: 'f1', userType: 'SUPPLIER' });

    await TestBed.configureTestingModule({
      imports: [OrderHistoryComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: OrderService, useValue: orderSpy },
        { provide: SupplierService, useValue: supplierSpy },
        { provide: UserService, useValue: userSpy },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error']) },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderHistoryComponent);
    fixture.detectChanges();
  };

  afterEach(() => {
    if (clockInstalled) jasmine.clock().uninstall();
    clockInstalled = false;
  });

  it('deveMostrarResumoDoBackend', async () => {
    await setup();

    expect(byId('summary-revenue')?.textContent).toMatch(/4[.,]523[.,]00/);
    expect(byId('summary-delivered')?.textContent).toContain('70');
    expect(byId('summary-canceled')?.textContent).toContain('4');
    expect(byId('summary-ticket')?.textContent).toMatch(/64[.,]61/);
    // padrão: últimos 30 dias, os dois status
    expect(orderSpy.getMyHistorySummary).toHaveBeenCalledWith(jasmine.objectContaining({ from: '2026-09-22', to: '2026-10-21', status: undefined }));
  });

  it('deveFiltrarPorStatusEPeriodo', async () => {
    await setup();

    click('status-canceled');
    expect(ultimoFiltro()).toEqual(jasmine.objectContaining({ status: 'canceled', page: 0 }));
    expect(byId('status-canceled')?.getAttribute('aria-pressed')).toBe('true');

    escolher('period-select', 'lastMonth');
    expect(ultimoFiltro()).toEqual(jasmine.objectContaining({ from: '2026-09-01', to: '2026-09-30' }));

    escolher('period-select', 'all');
    expect(ultimoFiltro().from).toBeUndefined();
    expect(ultimoFiltro().to).toBeUndefined();
  });

  it('deveFiltrarPorPagamentoEModo', async () => {
    await setup();

    escolher('payment-select', 'PIX');
    escolher('mode-select', 'PICKUP');

    expect(ultimoFiltro()).toEqual(jasmine.objectContaining({ payment: 'PIX', mode: 'PICKUP' }));
    expect(orderSpy.getMyHistorySummary.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({ payment: 'PIX', mode: 'PICKUP' }));
    expect(byId('active-filters')?.textContent).toContain('PIX');
    expect(byId('active-filters')?.textContent).toContain('Retirada');
  });

  it('deveBuscarComDebounce', fakeAsync(async () => {
    await setup(pagina([entregue, cancelado]), false);
    orderSpy.listMyHistory.calls.reset();
    const input = byId('history-search') as HTMLInputElement;

    input.value = 'maria';
    input.dispatchEvent(new Event('input'));
    tick(100);
    expect(orderSpy.listMyHistory).not.toHaveBeenCalled();
    tick(300);

    expect(orderSpy.listMyHistory).toHaveBeenCalledOnceWith(jasmine.objectContaining({ q: 'maria', page: 0 }));
  }));

  it('deveLimparFiltros', async () => {
    await setup();
    click('status-delivered');
    escolher('payment-select', 'PIX');

    click('clear-filters');

    expect(ultimoFiltro()).toEqual(jasmine.objectContaining({ status: undefined, payment: undefined, mode: undefined, from: '2026-09-22', to: '2026-10-21' }));
    expect(byId('active-filters')).toBeNull();
  });

  it('deveExpandirDetalhesDoPedido', async () => {
    await setup();
    const card = byId('order-' + entregue.id)!;
    expect(card.textContent).toContain('#A1B2C3D4');
    expect(card.textContent).toContain('Maria Souza');
    expect(byId('details-' + entregue.id)).toBeNull();

    click('toggle-' + entregue.id);

    const detalhes = byId('details-' + entregue.id)!;
    expect(detalhes.textContent).toContain('1 × Ultragaz · Gás 13kg');
    expect(detalhes.textContent).toContain('Rua XV, 700 · Centro · Curitiba');
    expect(detalhes.textContent).toContain('Carlos');
    expect(byId('toggle-' + entregue.id)?.getAttribute('aria-expanded')).toBe('true');
    // HU28 Cenário 4: o frete aparece entre os itens e o total
    expect(detalhes.querySelector('[data-testid="order-fee"]')).not.toBeNull();
  });

  it('deveMostrarMotivoDoCancelamento', async () => {
    await setup();

    click('toggle-' + cancelado.id);

    expect(byId('details-' + cancelado.id)?.textContent).toContain('Sem estoque do P13');
    expect(byId('details-' + cancelado.id)?.textContent).toContain('Retirada na loja');
  });

  it('devePaginar', async () => {
    await setup(pagina([entregue], { totalElements: 25, totalPages: 3 }));

    click('next-page');

    expect(ultimoFiltro()).toEqual(jasmine.objectContaining({ page: 1 }));
  });

  it('deveExibirEstadoVazio', async () => {
    await setup(pagina([]));

    expect(byId('empty-state')?.textContent).toContain('Nenhum pedido finalizado');
  });

  // ---- HU18 ----
  it('deveMostrarOEstadoDaLojaSemInterruptor', async () => {
    await setup();

    expect(fixture.nativeElement.querySelector('app-store-hours-status')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="store-toggle"]')).toBeNull();
  });
});
