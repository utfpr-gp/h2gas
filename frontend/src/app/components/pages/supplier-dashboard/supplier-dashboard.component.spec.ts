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
      { productName: 'Ultragaz Gás 13kg', quantity: 58, revenue: 7250 },
      { productName: 'Minalba Água 20L', quantity: 29, revenue: 435 },
    ],
    paymentBreakdown: [
      { method: 'PIX', orders: 63, revenue: 3400, percent: 75 },
      { method: 'DINHEIRO', orders: 21, revenue: 1123, percent: 25 },
    ],
    ...over,
  });

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid=${id}]`) as HTMLElement | null;
  const all = (id: string) => Array.from(fixture.nativeElement.querySelectorAll(`[data-testid=${id}]`)) as HTMLElement[];

  const setup = async (response = of(dashboard())) => {
    supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getDashboard']);
    supplierServiceSpy.getDashboard.and.returnValue(response);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['error']);
    await TestBed.configureTestingModule({
      imports: [SupplierDashboardComponent],
      providers: [
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
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
    expect(supplierServiceSpy.getDashboard).toHaveBeenCalledWith(select.options[1].value);
  });

  it('deveExibirEstadoVazioSemVendas', async () => {
    await setup(of(dashboard({
      totalRevenue: 0, totalOrders: 0, averageTicket: null, receivable: 0, deliveredOrders: 0,
      averageDeliveryMinutes: null, revenueChangePercent: null, ordersChangePercent: null,
      revenueByDay: [{ day: '2026-10-01', revenue: 0 }], salesByProduct: [], paymentBreakdown: [],
    })));

    expect(fixture.nativeElement.textContent).toContain('Sem vendas neste mês');
    expect(all('product-row').length).toBe(0);
  });

  it('deveAvisarQuandoFalharAoBuscarDashboard', async () => {
    await setup(throwError(() => ({ error: { message: 'Access Denied' } })) as any);

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('Access Denied'));
  });
});
