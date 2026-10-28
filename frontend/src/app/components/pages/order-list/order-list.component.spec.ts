import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ToastrService } from 'ngx-toastr';

import { OrderListComponent } from './order-list.component';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { CartService } from '../../../services/cart.service';
import { OrderStatusChange, OrderTrackingService } from '../../../services/order-tracking.service';
import { OrderResponse, ReorderItem, ReorderPreview } from '../../../models/order.model';

describe('OrderListComponent', () => {
  let component: OrderListComponent;
  let fixture: ComponentFixture<OrderListComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let trackingSpy: jasmine.SpyObj<OrderTrackingService>;
  let statusChange$: Subject<OrderStatusChange>;

  const pedido = { id: 'pedido-1', supplierName: 'Fornecedor', status: 'Em preparacao', orderItems: [] } as unknown as OrderResponse;

  const botijao: ReorderItem = {
    productId: 5, productBrand: 'Ultragaz', category: 'P13', type: 'Gás', imageUrl: 'img.png',
    supplierId: 'sup-1', quantity: 2, priceDelivery: 120, pricePickup: 100,
  };
  const agua: ReorderItem = {
    productId: 7, productBrand: 'Minalba', category: '20L', type: 'Água', supplierId: 'sup-1',
    quantity: 0, priceDelivery: 25, pricePickup: 20, unavailableReason: 'SEM_ESTOQUE',
  };

  function previewRetorna(preview: ReorderPreview) {
    orderServiceSpy.getReorderPreview.and.returnValue(of(preview));
  }

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['findByCustomerId', 'getReorderPreview']);
    orderServiceSpy.findByCustomerId.and.returnValue(of({
      data: [{ ...pedido }], page: 0, totalPages: 1, hasNext: false, hasPrevious: false,
    } as any));
    statusChange$ = new Subject<OrderStatusChange>();
    trackingSpy = jasmine.createSpyObj('OrderTrackingService', ['connect', 'disconnect'], { onStatusChange: statusChange$.asObservable() });
    cartServiceSpy = jasmine.createSpyObj('CartService', ['getCart', 'setCart']);
    cartServiceSpy.getCart.and.returnValue([]);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error', 'info']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'user-1' });

    await TestBed.configureTestingModule({
      imports: [OrderListComponent],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: Router, useValue: routerSpy },
        { provide: OrderTrackingService, useValue: trackingSpy },
        provideNoopAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // HU08 — atualização em tempo real via SSE
  const badge = () => fixture.nativeElement.querySelector('[data-testid=order-status]') as HTMLElement;
  const card = () => fixture.nativeElement.querySelector('[data-testid=order-card]') as HTMLElement;

  it('deveConectarAoCanalDoClienteAoAbrirATela', () => {
    expect(trackingSpy.connect).toHaveBeenCalledOnceWith('user-1');
  });

  it('deveAtualizarStatusDoPedidoEmTempoRealSemRefetchCompleto', () => {
    expect(badge().textContent).toContain('Em preparacao');

    statusChange$.next({ orderId: 'pedido-1', status: 'Saiu para entrega' });
    fixture.detectChanges();

    expect(badge().textContent).toContain('Saiu para entrega');
    expect(orderServiceSpy.findByCustomerId).toHaveBeenCalledTimes(1);
  });

  it('deveDestacarCardAtualizado', fakeAsync(() => {
    statusChange$.next({ orderId: 'pedido-1', status: 'Entregue' });
    fixture.detectChanges();
    expect(card().classList).toContain('status-updated');

    tick(2000);
    fixture.detectChanges();
    expect(card().classList).not.toContain('status-updated');
  }));

  it('deveIgnorarEventoDePedidoForaDaPagina', () => {
    statusChange$.next({ orderId: 'pedido-de-outra-pagina', status: 'Entregue' });
    fixture.detectChanges();

    expect(badge().textContent).toContain('Em preparacao');
    expect(card().classList).not.toContain('status-updated');
  });

  it('deveFecharEventSourceAoDestruirComponente', () => {
    fixture.destroy();

    expect(trackingSpy.disconnect).toHaveBeenCalled();
    statusChange$.next({ orderId: 'pedido-1', status: 'Entregue' });
    expect(component.orders?.[0].status as string).toBe('Em preparacao');
  });

  it('deveSubstituirCarrinhoComItensDisponiveisAoClicarPedirNovamente', () => {
    previewRetorna({ availableItems: [botijao], unavailableItems: [] });

    component.reorder(pedido);

    expect(orderServiceSpy.getReorderPreview).toHaveBeenCalledWith('pedido-1');
    expect(cartServiceSpy.setCart).toHaveBeenCalledWith([{ productId: 5, quantity: 2 }], 'sup-1');
  });

  // HU20 Cenário 7: pedir de novo grava só ids e quantidades
  it('deveGuardarSoIdsAoPedirDeNovo', () => {
    previewRetorna({ availableItems: [botijao], unavailableItems: [agua] });

    component.reorder(pedido);

    const [linhas, loja] = cartServiceSpy.setCart.calls.mostRecent().args;
    expect(linhas).toEqual([{ productId: 5, quantity: 2 }]);
    expect(Object.keys(linhas[0])).toEqual(['productId', 'quantity']);
    expect(loja).toBe('sup-1');
  });

  it('deveNotificarItensIndisponiveisSemBloquearOsDisponiveis', () => {
    previewRetorna({ availableItems: [botijao], unavailableItems: [agua] });

    component.reorder(pedido);

    expect(toastrSpy.warning).toHaveBeenCalledWith(jasmine.stringMatching('Minalba'));
    expect(cartServiceSpy.setCart).toHaveBeenCalledWith([{ productId: 5, quantity: 2 }], 'sup-1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/resumo-pedido']);
  });

  // F7 §5.10: busca não gera toast; uma ação gera um toast só
  it('naoDeveExibirToastDeSucessoAoBuscar', () => {
    expect(orderServiceSpy.findByCustomerId).toHaveBeenCalled();
    expect(toastrSpy.success).not.toHaveBeenCalled();
  });

  it('deveExibirUmUnicoAvisoAoRefazerComItensIndisponiveis', () => {
    cartServiceSpy.getCart.and.returnValue([{ productId: 99, quantity: 1 }]);
    previewRetorna({ availableItems: [botijao], unavailableItems: [agua] });

    component.reorder(pedido);

    expect(toastrSpy.info).not.toHaveBeenCalled();
    expect(toastrSpy.warning).toHaveBeenCalledOnceWith(jasmine.stringMatching(/carrinho.*Minalba/i));
  });

  it('deveNavegarParaResumoPedidoAposRecompra', () => {
    previewRetorna({ availableItems: [botijao], unavailableItems: [] });

    (fixture.nativeElement.querySelector('[data-testid="reorder"]') as HTMLButtonElement).click();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/resumo-pedido']);
  });

  it('naoDeveNavegarQuandoNenhumItemEstiverDisponivel', () => {
    previewRetorna({ availableItems: [], unavailableItems: [agua] });

    component.reorder(pedido);

    expect(cartServiceSpy.setCart).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(toastrSpy.warning).toHaveBeenCalled();
  });

  it('deveDesabilitarPedirNovamenteEnquantoCarrega', () => {
    const resposta = new Subject<ReorderPreview>();
    orderServiceSpy.getReorderPreview.and.returnValue(resposta);
    const botao = () => fixture.nativeElement.querySelector('[data-testid="reorder"]') as HTMLButtonElement;

    botao().click();
    fixture.detectChanges();

    expect(component.reorderingId).toBe('pedido-1');
    expect(botao().disabled).toBeTrue();

    resposta.next({ availableItems: [botijao], unavailableItems: [] });
    fixture.detectChanges();

    expect(component.reorderingId).toBeNull();
  });

  it('deveExibirRetiradaNaLojaQuandoPedidoForPickup', () => {
    orderServiceSpy.findByCustomerId.and.returnValue(of({
      data: [{ ...pedido, deliveryMode: 'PICKUP' }], page: 0, totalPages: 1, hasNext: false, hasPrevious: false,
    } as any));

    component.getOrders();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Retirada na loja');
  });
});
