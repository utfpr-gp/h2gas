import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ReceivedOrdersComponent } from './received-orders.component';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { SupplierService } from '../../../services/supplier.service';
import { OrderResponse } from '../../../models/order.model';

describe('ReceivedOrdersComponent', () => {
  let component: ReceivedOrdersComponent;
  let fixture: ComponentFixture<ReceivedOrdersComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const pedido = (id: string, status: string) =>
    ({ id, status, supplierName: 'Gás Centro', customerName: 'Maria', totalPrice: 125, orderItems: [] } as unknown as OrderResponse);
  const lista = (...orders: OrderResponse[]) =>
    of({ data: orders, page: 0, totalPages: 1, hasNext: false, hasPrevious: false } as any);

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService',
      ['findByExcludeStatus', 'findBySupplierId', 'updateStatusOrder', 'cancelOrder']);
    orderServiceSpy.findByExcludeStatus.and.returnValue(
      lista(pedido('c1', 'Confirmado'), pedido('p1', 'Pendente'), pedido('p2', 'Pendente')));
    orderServiceSpy.updateStatusOrder.and.returnValue(of({ status: 'Confirmado' }));
    orderServiceSpy.cancelOrder.and.returnValue(of({ status: 'Cancelado', customerNotified: true }));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error', 'info']);
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'sup-1', supplierId: 'sup-1' });
    const supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getOpen', 'openDistribuidor']);
    supplierServiceSpy.getOpen.and.returnValue(of(true));

    await TestBed.configureTestingModule({
      imports: [ReceivedOrdersComponent],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideRouter([]),
        provideNoopAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReceivedOrdersComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => fixture.destroy());

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('deveSepararPedidosPendentesNoTopoComContagem', () => {
    fixture.detectChanges();

    expect(component.pendingOrders.map((o) => o.id)).toEqual(['p1', 'p2']);
    expect(component.otherOrders.map((o) => o.id)).toEqual(['c1']);
  });

  it('deveSelecionarPrimeiroPendentePorPadrao', () => {
    fixture.detectChanges();

    expect(component.selectedOrder?.id).toBe('p1');
  });

  it('deveConfirmarPedidoPendenteComUmClique', () => {
    fixture.detectChanges();
    const p1 = component.pendingOrders[0];

    component.changeStatus(p1, 'CONFIRMED');

    expect(orderServiceSpy.updateStatusOrder).toHaveBeenCalledOnceWith({ orderStatus: 'CONFIRMED' }, 'p1');
    expect(p1.status).toBe('Confirmado');
  });

  it('naoDevePermitirCancelarSemJustificativa', () => {
    fixture.detectChanges();
    component.openCancel(component.pendingOrders[0]);
    component.cancelReason = '   ';

    component.confirmCancel();

    expect(orderServiceSpy.cancelOrder).not.toHaveBeenCalled();
    expect(component.cancelTarget?.id).toBe('p1');
  });

  it('deveChamarCancelOrderComMotivoInformado', () => {
    fixture.detectChanges();
    component.openCancel(component.pendingOrders[0]);
    component.cancelReason = '  Sem gás no estoque  ';

    component.confirmCancel();

    expect(orderServiceSpy.cancelOrder).toHaveBeenCalledOnceWith('p1', 'Sem gás no estoque');
    expect(component.orders?.map((o) => o.id)).not.toContain('p1');
    expect(component.cancelTarget).toBeUndefined();
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('deveAvisarQuandoClienteNaoFoiNotificado', () => {
    orderServiceSpy.cancelOrder.and.returnValue(of({ status: 'Cancelado', customerNotified: false }));
    fixture.detectChanges();
    component.openCancel(component.pendingOrders[0]);
    component.cancelReason = 'Sem entregador';

    component.confirmCancel();

    expect(toastrSpy.warning).toHaveBeenCalledWith(jasmine.stringMatching('não foi avisado'));
  });

  it('deveAtualizarPedidosPeriodicamente', fakeAsync(() => {
    fixture.detectChanges();
    expect(orderServiceSpy.findByExcludeStatus).toHaveBeenCalledTimes(1);

    orderServiceSpy.findByExcludeStatus.and.returnValue(
      lista(pedido('p3', 'Pendente'), pedido('c1', 'Confirmado'), pedido('p1', 'Pendente'), pedido('p2', 'Pendente')));
    tick(30000);

    expect(orderServiceSpy.findByExcludeStatus).toHaveBeenCalledTimes(2);
    expect(component.isNew(component.orders!.find((o) => o.id === 'p3')!)).toBeTrue();
    expect(component.isNew(component.orders!.find((o) => o.id === 'p1')!)).toBeFalse();
    expect(component.selectedOrder?.id).toBe('p1');

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('naoDeveExibirToastDeSucessoAoBuscarPedidos', () => {
    fixture.detectChanges();

    expect(toastrSpy.success).not.toHaveBeenCalled();
  });
});
