import { Subject } from 'rxjs';
import { SupplierLiveService, SupplierLiveEvent } from '../../../services/supplier-live.service';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SupplierHomeComponent } from './supplier-home.component';
import { SupplierService } from '../../../services/supplier.service';
import { ProductItemService } from '../../../services/product-item.service';
import { DelivererService } from '../../../services/deliverer.service';
import { UserService } from '../../../services/user.service';

// F9: tela inicial do fornecedor, com um card por área e o número que importa agora
describe('SupplierHomeComponent', () => {
  // HU27 (tempo real da loja): aviso do canal SSE da loja
  let live$ = new Subject<SupplierLiveEvent>();
  beforeEach(() => (live$ = new Subject<SupplierLiveEvent>()));

  let fixture: ComponentFixture<SupplierHomeComponent>;
  let supplierSpy: jasmine.SpyObj<SupplierService>;
  let productSpy: jasmine.SpyObj<ProductItemService>;
  let delivererSpy: jasmine.SpyObj<DelivererService>;

  const card = (id: string) => fixture.nativeElement.querySelector(`[data-testid="home-card-${id}"]`) as HTMLAnchorElement | null;
  const valor = (id: string) => card(id)?.querySelector('[data-testid="home-card-value"]')?.textContent?.trim();

  const setup = async (falhar: 'dashboard' | 'estoque' | null = null) => {
    supplierSpy = jasmine.createSpyObj('SupplierService', ['getDashboard', 'getOpen', 'getMySettings']);
    supplierSpy.getDashboard.and.returnValue(falhar === 'dashboard'
      ? throwError(() => ({ status: 500 }))
      : of({ pendingOrders: 3, outForDeliveryOrders: 1, totalRevenue: 4210.5 } as any));
    supplierSpy.getOpen.and.returnValue(of(true));
    supplierSpy.getMySettings.and.returnValue(of({ openingTime: '08:00', closingTime: '18:00', averageDeliveryTime: 30, lowStockThreshold: 8, offersDelivery: true, offersPickup: true, deliveryRadiusKm: null, deliveryBaseFee: 0, deliveryFeePerKm: 0 }));
    productSpy = jasmine.createSpyObj('ProductItemService', ['getStockSummary']);
    productSpy.getStockSummary.and.returnValue(falhar === 'estoque'
      ? throwError(() => ({ status: 500 }))
      : of({ total: 10, available: 8, lowStock: 2, outOfStock: 1, lowStockThreshold: 8, offersDelivery: true, offersPickup: true, deliveryRadiusKm: null, deliveryBaseFee: 0, deliveryFeePerKm: 0 }));
    delivererSpy = jasmine.createSpyObj('DelivererService', ['listMine']);
    delivererSpy.listMine.and.returnValue(of([
      { id: 1, name: 'Ana', email: 'a@x', phoneNumber: '1', active: true, busy: false },
      { id: 2, name: 'Beto', email: 'b@x', phoneNumber: '2', active: true, busy: true },
      { id: 3, name: 'Caio', email: 'c@x', phoneNumber: '3', active: false, busy: false },
    ]));
    const userSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userSpy.getUserLocalStorage.and.returnValue({ id: 'forn-1', name: 'Gás Centro', supplierId: 'forn-1', userType: 'SUPPLIER' });

    await TestBed.configureTestingModule({
      imports: [SupplierHomeComponent],
      providers: [
        { provide: SupplierLiveService, useValue: { changes: () => live$.asObservable() } },
        provideRouter([]),
        { provide: SupplierService, useValue: supplierSpy },
        { provide: ProductItemService, useValue: productSpy },
        { provide: DelivererService, useValue: delivererSpy },
        { provide: UserService, useValue: userSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SupplierHomeComponent);
    fixture.detectChanges();
  };

  it('deveMostrarOsSeisCards', async () => {
    await setup();

    for (const id of ['pedidos', 'painel', 'produtos', 'entregadores', 'loja', 'historico']) {
      expect(card(id)).withContext(id).not.toBeNull();
    }
  });

  it('deveLevarCadaCardParaATela', async () => {
    await setup();

    expect(card('pedidos')!.getAttribute('href')).toBe('/minha-loja/pedidos');
    expect(card('painel')!.getAttribute('href')).toBe('/minha-loja/painel');
    expect(card('produtos')!.getAttribute('href')).toBe('/minha-loja/produtos');
    expect(card('entregadores')!.getAttribute('href')).toBe('/minha-loja/entregadores');
    expect(card('loja')!.getAttribute('href')).toBe('/minha-loja/configuracoes');
    expect(card('historico')!.getAttribute('href')).toBe('/minha-loja/pedidos/historico');
  });

  it('deveMostrarOsPendentesEOFaturamento', async () => {
    await setup();

    expect(valor('pedidos')).toBe('3');
    expect(card('pedidos')!.textContent).toContain('pendentes');
    expect(card('pedidos')!.textContent).toContain('1 em rota');
    expect(valor('painel')).toMatch(/4[.,]?210[.,]50/);
    // HU28 Cenário 3: a receita do painel é só do que foi entregue
    expect(card('painel')!.textContent).toContain('recebido no mês');
  });

  it('deveMostrarOEstoqueBaixo', async () => {
    await setup();

    expect(valor('produtos')).toBe('3');
    expect(card('produtos')!.textContent).toContain('estoque baixo ou zerado');
  });

  it('deveMostrarEntregadoresLivres', async () => {
    await setup();

    expect(valor('entregadores')).toBe('1 de 2');
    expect(card('entregadores')!.textContent).toContain('livre');
  });

  it('deveMostrarSeALojaEstaAberta', async () => {
    await setup();

    expect(valor('loja')).toBe('Aberta');
    expect(card('loja')!.textContent).toContain('fecha às 18:00');
    // o card leva às configurações e diz isso (pedido do Igor: "precisa da configuração também")
    expect(card('loja')!.textContent).toContain('Configurações');
    expect(card('loja')!.textContent).toContain('horário, dados, endereço e estoque');
  });

  it('deveMostrarTracoSeUmaFonteFalhar', async () => {
    await setup('dashboard');

    expect(valor('pedidos')).toBe('—');
    expect(valor('painel')).toBe('—');
    expect(valor('produtos')).toBe('3');
    expect(valor('entregadores')).toBe('1 de 2');
  });

  it('deveCumprimentarALoja', async () => {
    await setup();

    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Gás Centro');
  });

  it('deveRecarregarOsNumerosQuandoChegaPedidoNovo', async () => {
    await setup();
    live$.next({ type: 'order-placed', orderId: 'novo-1' });

    expect(supplierSpy.getDashboard).toHaveBeenCalledTimes(2);
  });
});
