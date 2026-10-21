import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { SupplierDashboardComponent } from './supplier-dashboard.component';
import { SupplierService } from '../../../services/supplier.service';
import { SupplierDashboard } from '../../../models/supplier-dashboard';
import { ProductItemService } from '../../../services/product-item.service';
import { CategoryProductService } from '../../../services/category-product.service';
import { ProductTypeService } from '../../../services/product-type.service';

registerLocaleData(localePt);

describe('SupplierDashboardComponent', () => {
  let fixture: ComponentFixture<SupplierDashboardComponent>;
  let component: SupplierDashboardComponent;
  let supplierServiceSpy: jasmine.SpyObj<SupplierService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const dashboard = (over: Partial<SupplierDashboard> = {}): SupplierDashboard => ({
    month: '2026-10',
    totalRevenue: 4523,
    revenueChangePercent: 12.5,
    totalOrders: 84,
    ordersChangePercent: -3.2,
    averageTicket: 53.85,
    receivable: 820,
    deliveredOrders: 70,
    pendingOrders: 3,
    outForDeliveryOrders: 2,
    averageDeliveryMinutes: 34.5,
    revenueByDay: [
      { day: '2026-10-01', revenue: 100 },
      { day: '2026-10-02', revenue: 0 },
      { day: '2026-10-03', revenue: 300 },
    ],
    salesByProduct: [
      { productId: 4, productName: 'Ultragaz Gás 13kg', quantity: 58, revenue: 7250 },
      { productId: 12, productName: 'Minalba Água 20L', quantity: 29, revenue: 435 },
    ],
    paymentBreakdown: [
      { method: 'PIX', orders: 63, revenue: 3400, percent: 75 },
      { method: 'DINHEIRO', orders: 21, revenue: 1123, percent: 25 },
    ],
    salesByCategory: [
      { productTypeId: 2, categoryName: 'Gás', quantity: 58, revenue: 7250, percent: 75 },
      { productTypeId: 1, categoryName: 'Água', quantity: 29, revenue: 435, percent: 25 },
    ],
    salesByStoreCategory: [
      { categoryId: 1, categoryName: 'Gás 13kg', typeName: 'Gás', quantity: 58, revenue: 7250, percent: 94.3 },
      { categoryId: 7, categoryName: 'Água 20L', typeName: 'Água', quantity: 29, revenue: 435, percent: 5.7 },
    ],
    deliveryModeBreakdown: [
      { mode: 'DELIVERY', orders: 70, revenue: 4000, percent: 83.3 },
      { mode: 'PICKUP', orders: 14, revenue: 523, percent: 16.7 },
    ],
    ...over,
  });

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid=${id}]`) as HTMLElement | null;
  const all = (id: string) => Array.from(fixture.nativeElement.querySelectorAll(`[data-testid=${id}]`)) as HTMLElement[];

  const setup = async (response = of(dashboard())) => {
    supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getDashboard']);
    supplierServiceSpy.getDashboard.and.returnValue(response);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['error']);
    const productSpy = jasmine.createSpyObj('ProductItemService', ['listMine']);
    productSpy.listMine.and.returnValue(of({ data: [{ id: 4, brand: 'Ultragaz', category: 'Gás 13kg' }, { id: 12, brand: 'Minalba', category: 'Água 20L' }],
      page: 0, size: 50, totalElements: 2, totalPages: 1, hasNext: false, hasPrevious: false }));
    const categorySpy = jasmine.createSpyObj('CategoryProductService', ['listMine']);
    categorySpy.listMine.and.returnValue(of([{ id: 1, name: 'Gás 13kg', type: 'Gás', productTypeId: 2, productCount: 2 },
      { id: 7, name: 'Água 20L', type: 'Água', productTypeId: 1, productCount: 1 }]));
    const typeSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    typeSpy.findAll.and.returnValue(of([{ id: 1, name: 'Água' }, { id: 2, name: 'Gás' }]));
    await TestBed.configureTestingModule({
      imports: [SupplierDashboardComponent],
      providers: [
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: ProductItemService, useValue: productSpy },
        { provide: CategoryProductService, useValue: categorySpy },
        { provide: ProductTypeService, useValue: typeSpy },
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        provideRouter([]),
        provideNoopAnimations(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(SupplierDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  afterEach(() => fixture.destroy());

  it('deveExibirKpisDoMes', async () => {
    await setup();

    expect(byId('kpi-revenue')!.textContent).toMatch(/R\$\s4\.523,00/);
    expect(byId('revenue-change')!.textContent).toContain('12,5');
    expect(byId('orders-change')!.textContent).toContain('3,2');
    expect(byId('kpi-orders')!.textContent).toContain('84');
    expect(byId('kpi-ticket')!.textContent).toMatch(/R\$\s53,85/);
    expect(byId('kpi-receivable')!.textContent).toMatch(/R\$\s820,00/);
    expect(byId('kpi-pending')!.textContent).toContain('3');
    expect(byId('kpi-route')!.textContent).toContain('2');
    expect(byId('kpi-delivered')!.textContent).toContain('70');
    expect(byId('kpi-avg-delivery')!.textContent).toContain('34,5');
    expect(supplierServiceSpy.getDashboard).toHaveBeenCalledTimes(1);
  });

  it('deveRenderizarBarrasDeVendasPorDia', async () => {
    await setup();

    const barras = all('day-bar');
    expect(barras.length).toBe(3);
    expect(barras[2].getAttribute('aria-label')).toMatch(/03\/10.*R\$\s300,00/);
    const alturas = barras.map((b) => parseFloat(b.style.height));
    expect(alturas[2]).toBe(100);
    expect(alturas[0]).toBeCloseTo(33.3, 0);
    expect(alturas[1]).toBe(0);
  });

  it('deveExibirVendasPorProdutoEPagamentos', async () => {
    await setup();

    const produtos = all('product-row');
    expect(produtos.length).toBe(2);
    expect(produtos[0].textContent).toContain('Ultragaz Gás 13kg');
    expect(produtos[0].textContent).toContain('58');
    const pagamentos = all('payment-row');
    expect(pagamentos.length).toBe(2);
    expect(pagamentos[0].textContent).toContain('PIX');
    expect(pagamentos[0].textContent).toContain('75');
    expect(pagamentos[1].textContent).toContain('Dinheiro');
  });

  it('deveRenderizarPizzaDeVendasPorCategoria', async () => {
    await setup();

    const fatias = all('category-slice');
    expect(fatias.length).toBe(2);
    expect(fatias[0].getAttribute('stroke-dasharray')).toBe('75 25');
    expect(fatias[1].getAttribute('stroke-dasharray')).toBe('25 75');
    expect(fatias[1].getAttribute('stroke-dashoffset')).toBe('-50');
    const legenda = all('category-legend-row');
    expect(legenda.length).toBe(2);
    expect(legenda[0].textContent).toContain('Gás');
    expect(legenda[0].textContent).toContain('75');
    expect(legenda[0].textContent).toMatch(/R\$\s7\.250,00/);
    expect(legenda[1].textContent).toContain('Água');
  });

  it('deveOcultarPizzaSemVendas', async () => {
    await setup(of(dashboard({
      totalRevenue: 0, totalOrders: 0, averageTicket: null, salesByCategory: [], salesByProduct: [], paymentBreakdown: [],
      revenueByDay: [{ day: '2026-10-01', revenue: 0 }],
    })));

    expect(all('category-slice').length).toBe(0);
    expect(byId('category-pie')).toBeNull();
  });

  it('deveExibirTracoQuandoTempoMedioForNulo', async () => {
    await setup(of(dashboard({ averageDeliveryMinutes: null, averageTicket: null, revenueChangePercent: null })));

    expect(byId('kpi-avg-delivery')!.textContent).toContain('—');
    expect(byId('kpi-ticket')!.textContent).toContain('—');
    expect(byId('revenue-change')).toBeNull();
  });

  it('deveRecarregarAoTrocarOMes', async () => {
    await setup();
    const select = byId('month-select') as unknown as HTMLSelectElement;
    expect(select.options.length).toBe(12);

    select.value = select.options[1].value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(supplierServiceSpy.getDashboard).toHaveBeenCalledTimes(2);
    expect(supplierServiceSpy.getDashboard).toHaveBeenCalledWith(select.options[1].value, {});
  });

  it('deveExibirEstadoVazioSemVendas', async () => {
    await setup(of(dashboard({
      totalRevenue: 0, totalOrders: 0, averageTicket: null, receivable: 0, deliveredOrders: 0,
      averageDeliveryMinutes: null, revenueChangePercent: null, ordersChangePercent: null,
      revenueByDay: [{ day: '2026-10-01', revenue: 0 }], salesByProduct: [], paymentBreakdown: [], salesByCategory: [],
    })));

    expect(fixture.nativeElement.textContent).toContain('Sem vendas neste mês');
    expect(all('product-row').length).toBe(0);
  });

  it('deveAvisarQuandoFalharAoBuscarDashboard', async () => {
    await setup(throwError(() => ({ error: { message: 'Access Denied' } })) as any);

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('Access Denied'));
  });

  // ---- F7 fatia 6: filtros e gráficos clicáveis ----
  const ultimoFiltro = () => supplierServiceSpy.getDashboard.calls.mostRecent().args[1];
  const clicar = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const escolher = (id: string, valor: string) => {
    const el = byId(id) as HTMLSelectElement;
    el.value = valor;
    el.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  it('deveFiltrarAoClicarNoPagamento', async () => {
    await setup();

    clicar('payment-PIX');

    expect(ultimoFiltro()).toEqual({ payment: 'PIX' });
    expect(byId('payment-PIX')!.getAttribute('aria-pressed')).toBe('true');
    expect(byId('payment-DINHEIRO')!.getAttribute('aria-pressed')).toBe('false');
  });

  it('deveTirarOFiltroAoClicarDeNovo', async () => {
    await setup();

    clicar('payment-PIX');
    clicar('payment-PIX');

    expect(ultimoFiltro()).toEqual({});
  });

  it('deveFiltrarAoClicarNoTipoCategoriaProdutoEModo', async () => {
    await setup();

    clicar('type-2');
    expect(ultimoFiltro()).toEqual({ typeId: 2 });
    clicar('store-category-7');
    expect(ultimoFiltro()).toEqual({ typeId: 2, categoryId: 7 });
    clicar('product-4');
    expect(ultimoFiltro()).toEqual({ typeId: 2, categoryId: 7, productId: 4 });
    clicar('mode-PICKUP');
    expect(ultimoFiltro()).toEqual({ typeId: 2, categoryId: 7, productId: 4, mode: 'PICKUP' });
  });

  it('deveFiltrarPelosSelects', async () => {
    await setup();

    escolher('filter-payment', 'DINHEIRO');
    escolher('filter-mode', 'DELIVERY');
    escolher('filter-type', '1');
    escolher('filter-category', '7');
    escolher('filter-product', '12');

    expect(ultimoFiltro()).toEqual({ payment: 'DINHEIRO', mode: 'DELIVERY', typeId: 1, categoryId: 7, productId: 12 });
    expect(Array.from((byId('filter-product') as HTMLSelectElement).options).map((o) => o.textContent!.trim()))
      .toEqual(['Todos os produtos', 'Ultragaz · Gás 13kg', 'Minalba · Água 20L']);
  });

  it('deveMostrarChipsELimpar', async () => {
    await setup();
    clicar('payment-PIX');
    clicar('type-2');

    const chips = byId('active-filters')!.textContent!;
    expect(chips).toContain('PIX');
    expect(chips).toContain('Gás');

    clicar('clear-filters');

    expect(ultimoFiltro()).toEqual({});
    expect(byId('active-filters')).toBeNull();
  });

  it('deveDestacarOMelhorDiaNoGrafico', async () => {
    await setup();

    expect(byId('best-day')!.textContent).toMatch(/03\/10/);
    expect(byId('best-day')!.textContent).toMatch(/R\$\s300,00/);
    const barras = all('day-bar');
    expect(barras[2].getAttribute('data-best')).toBe('true');
    expect(barras[0].getAttribute('data-best')).toBe('false');
    // o melhor dia não ganha cor própria (parecia outra série); fica no texto e no número em negrito
    expect(barras.every((b) => b.classList.contains('bg-primary') && !b.classList.contains('bg-chart-2'))).toBeTrue();
    expect(all('axis-label').length).toBeGreaterThan(1);
  });

  it('deveMostrarTotalNoCentroDaPizza', async () => {
    await setup();

    expect(byId('pie-total')!.textContent).toMatch(/R\$\s7\.685,00/);
  });

  it('deveExplicarVazioCausadoPeloFiltro', async () => {
    await setup();
    supplierServiceSpy.getDashboard.and.returnValue(of(dashboard({ totalOrders: 0, totalRevenue: 0, revenueByDay: [],
      salesByProduct: [], paymentBreakdown: [], salesByCategory: [], salesByStoreCategory: [], deliveryModeBreakdown: [] })));

    escolher('filter-payment', 'CREDIT_CARD');

    expect(byId('empty-state')!.textContent).toContain('Nenhuma venda com estes filtros');
    clicar('empty-clear-filters');
    expect(ultimoFiltro()).toEqual({});
  });

  it('deveLevarParaACentralDePedidos', async () => {
    await setup();

    const atalho = byId('orders-center-link') as HTMLAnchorElement;
    expect(atalho.textContent).toContain('Central de pedidos');
    expect(atalho.getAttribute('href')).toBe('/dashboard-fornecedor');
    expect(byId('kpi-pending-link')!.getAttribute('href')).toBe('/dashboard-fornecedor');
    expect(byId('kpi-route-link')!.getAttribute('href')).toBe('/dashboard-fornecedor');
    expect(byId('kpi-pending-link')!.textContent).toContain('3');
  });

  it('deveMostrarCadaDiaDoMesNoEixo', async () => {
    await setup();

    const dias = all('day-label');
    expect(dias.map((d) => d.textContent!.trim())).toEqual(['1', '2', '3']);
    // no celular só os ímpares aparecem; os pares ficam para telas maiores
    // invisible (e não hidden) mantém o espaço do rótulo, então cada número fica embaixo da sua coluna
    expect(dias[1].classList).toContain('invisible');
    expect(dias[1].classList).toContain('sm:visible');
    expect(dias[0].classList).not.toContain('invisible');
  });

  it('deveMostrarOMelhorDiaMesmoQuandoForPar', async () => {
    await setup(of(dashboard({ revenueByDay: [
      { day: '2026-10-01', revenue: 100 }, { day: '2026-10-02', revenue: 900 }, { day: '2026-10-03', revenue: 300 },
    ] })));

    const dias = all('day-label');
    expect(dias[1].classList).not.toContain('invisible');
    expect(dias[1].classList).toContain('font-bold');
  });
});
