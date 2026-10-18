import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';
import { User } from '../../../models/user';
import { OrderResponse } from '../../../models/order.model';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { OrderStatus } from '../../../models/order-status.enum';
import { SupplierService } from '../../../services/supplier.service';
import { DelivererService } from '../../../services/deliverer.service';
import { Deliverer } from '../../../models/deliverer';
import { OrderStatusTimelineComponent } from '../../order-status-timeline/order-status-timeline.component';
import { fadeIn, staggerList } from '../../../animations/checkout.animations';

@Component({
  selector: 'app-received-orders',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    OrderStatusTimelineComponent
  ],
  templateUrl: './received-orders.component.html',
  animations: [fadeIn, staggerList],
})

export class ReceivedOrdersComponent implements OnInit, OnDestroy {

  constructor(
    private orderService: OrderService,
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router,
    private supplierService: SupplierService,
    private delivererService: DelivererService,
  ) { }

  userLogado?: User;
  orders?: OrderResponse[];
  orderStatus = OrderStatus;
  showSupplier = false;

  // HU06: fila com pendentes no topo, pedido selecionado no detalhe (Figura 14)
  selectedOrder?: OrderResponse;
  statusFilter = 'ALL';
  cancelTarget?: OrderResponse;
  cancelReason = '';
  canceling = false;
  showDetailMobile = false;

  // HU07: despacho com entregador (Figura 15)
  dispatchTarget?: OrderResponse;
  deliverers: Deliverer[] = [];
  loadingDeliverers = false;
  selectedDelivererId?: number;
  dispatching = false;
  readonly refreshIntervalMs = 30000;
  private knownOrderIds = new Set<string>();
  private newOrderIds = new Set<string>();
  private refreshSub?: Subscription;

  actionsByStatus: Record<OrderStatus, { label: string, icon: string, color: string, action: string }[]> = {
    [OrderStatus.PENDING]: [
      { label: 'Confirmar', icon: 'fa-check', color: 'bg-blue-600 hover:bg-blue-700', action: 'CONFIRMED' },
      { label: 'Cancelar', icon: 'fa-xmark', color: 'bg-red-600 hover:bg-red-700', action: 'CANCELED' }
    ],
    [OrderStatus.CONFIRMED]: [
      { label: 'Preparar', icon: 'fa-box', color: 'bg-yellow-600 hover:bg-yellow-700', action: 'IN_PREPARATION' },
      { label: 'Cancelar', icon: 'fa-xmark', color: 'bg-red-600 hover:bg-red-700', action: 'CANCELED' }
    ],
    [OrderStatus.IN_PREPARATION]: [
      { label: 'Enviar para entrega', icon: 'fa-truck', color: 'bg-indigo-600 hover:bg-indigo-700', action: 'OUT_FOR_DELIVERY' }
    ],
    [OrderStatus.OUT_FOR_DELIVERY]: [
      { label: 'Marcar como entregue', icon: 'fa-circle-check', color: 'bg-green-600 hover:bg-green-700', action: 'DELIVERED' }
    ],
    [OrderStatus.DELIVERED]: [],
    [OrderStatus.CANCELED]: []
  };

  ngOnInit() {
    this.userLogado = this.userService.getUserLocalStorage();
    this.loadOrders();
    this.getOpenStatus();
    // a HU08 (SSE) não cobre o painel do fornecedor: pedido novo chega por atualização periódica
    this.refreshSub = interval(this.refreshIntervalMs).subscribe(() => this.loadOrders());
  }

  ngOnDestroy() {
    this.refreshSub?.unsubscribe();
  }

  get pendingOrders(): OrderResponse[] {
    return this.orders?.filter((o) => o.status === OrderStatus.PENDING) ?? [];
  }

  get otherOrders(): OrderResponse[] {
    return this.orders?.filter((o) => o.status !== OrderStatus.PENDING) ?? [];
  }

  isNew(order: OrderResponse): boolean {
    return this.newOrderIds.has(order.id);
  }

  selectOrder(order?: OrderResponse) {
    this.selectedOrder = order;
    this.showDetailMobile = !!order;
    if (order) {
      this.newOrderIds.delete(order.id);
    }
  }

  // pedido de entrega em preparo sai por despacho com entregador; retirada segue com o atalho
  actionsFor(order: OrderResponse): { label: string; icon: string; color: string; action: string }[] {
    const actions = this.actionsByStatus[order.status as OrderStatus] ?? [];
    return actions.map((action) =>
      action.action === 'OUT_FOR_DELIVERY' && order.deliveryMode !== 'PICKUP'
        ? { ...action, label: order.delivererName ? 'Trocar entregador' : 'Despachar', action: 'DISPATCH' }
        : action);
  }

  // na retirada, "saiu para entrega" e "entregue" mudam de sentido
  actionLabel(order: OrderResponse, action: { label: string; action: string }): string {
    if (order.deliveryMode === 'PICKUP') {
      if (action.action === 'OUT_FOR_DELIVERY') return 'Pronto para retirada';
      if (action.action === 'DELIVERED') return 'Marcar como retirado';
    }
    return action.label;
  }

  onStatusChange(status: string) {
    this.statusFilter = status;
    this.loadOrders();
  }

  loadOrders() {
    const supplierId = this.userLogado?.supplierId!;
    const request = this.statusFilter === 'ALL'
      ? this.orderService.findByExcludeStatus(supplierId, ['DELIVERED', 'CANCELED'])
      : this.orderService.findBySupplierId(supplierId, this.statusFilter);
    request.subscribe({
      next: (response) => this.applyOrders(response.data),
      error: (err) => this.toastr.error(`Erro ao buscar os pedidos: ${this.backendMessage(err)}`),
    });
  }

  private applyOrders(orders: OrderResponse[]) {
    const firstLoad = this.knownOrderIds.size === 0 && !this.orders;
    for (const order of orders) {
      if (!firstLoad && !this.knownOrderIds.has(order.id) && order.status === OrderStatus.PENDING) {
        this.newOrderIds.add(order.id);
      }
      this.knownOrderIds.add(order.id);
    }
    this.orders = orders;
    const stillThere = this.selectedOrder && orders.find((o) => o.id === this.selectedOrder!.id);
    this.selectedOrder = stillThere || this.pendingOrders[0] || orders[0];
  }

  onToggleSupplier(checked: any) {
    this.showSupplier = checked.target.checked;
    this.supplierService.openDistribuidor({ status: this.showSupplier }, this.userLogado?.supplierId!).subscribe({
      next: () => this.toastr.success(this.showSupplier ? 'Distribuidora aberta.' : 'Distribuidora fechada.'),
      error: (err) => this.toastr.error(`Erro ao abrir distribuidora: ${this.backendMessage(err)}`),
    });
  }

  getOpenStatus() {
    this.supplierService.getOpen(this.userLogado?.supplierId!).subscribe({
      next: (response) => this.showSupplier = response,
      error: (err) => this.toastr.error(`Erro ao buscar a distribuidora: ${this.backendMessage(err)}`),
    });
  }

  changeStatus(order: OrderResponse, action: string) {
    if (action === 'CANCELED') {
      this.openCancel(order);
      return;
    }
    if (action === 'DISPATCH') {
      this.openDispatch(order);
      return;
    }
    this.orderService.updateStatusOrder({ orderStatus: action }, order.id).subscribe({
      next: (response: any) => {
        order.status = response.status;
        this.toastr.success(`Pedido atualizado para: ${response.status}`);
        if (order.status === OrderStatus.DELIVERED) {
          this.removeOrder(order);
        }
      },
      error: (err) => this.toastr.error(`Erro ao atualizar o pedido: ${this.backendMessage(err)}`),
    });
  }

  openDispatch(order: OrderResponse) {
    this.dispatchTarget = order;
    this.selectedDelivererId = undefined;
    this.deliverers = [];
    this.loadingDeliverers = true;
    this.delivererService.listMine().subscribe({
      next: (frota) => {
        this.deliverers = frota.filter((d) => d.active);
        this.loadingDeliverers = false;
      },
      error: (err) => {
        this.loadingDeliverers = false;
        this.toastr.error(`Erro ao buscar entregadores: ${this.backendMessage(err)}`);
      },
    });
  }

  closeDispatch() {
    this.dispatchTarget = undefined;
    this.selectedDelivererId = undefined;
  }

  selectDeliverer(deliverer: Deliverer) {
    this.selectedDelivererId = deliverer.id;
  }

  confirmDispatch() {
    if (!this.dispatchTarget || this.selectedDelivererId == null || this.dispatching) {
      return;
    }
    const order = this.dispatchTarget;
    this.dispatching = true;
    this.orderService.assignDeliverer(order.id, this.selectedDelivererId).subscribe({
      next: (response) => {
        this.dispatching = false;
        order.status = response.status;
        order.delivererName = response.delivererName;
        order.lastRefusalReason = undefined;
        order.lastRefusalBy = undefined;
        this.closeDispatch();
        this.toastr.success(`${response.delivererName} foi atribuído ao pedido. Ele inicia a entrega pelo app.`);
      },
      error: (err) => {
        this.dispatching = false;
        this.toastr.error(`Erro ao despachar: ${this.backendMessage(err)}`);
      },
    });
  }

  goToDeliverers() {
    this.closeDispatch();
    this.router.navigate(['/entregadores']);
  }

  // Cenário 2: cancelar só com justificativa
  openCancel(order: OrderResponse) {
    this.cancelTarget = order;
    this.cancelReason = '';
  }

  closeCancel() {
    this.cancelTarget = undefined;
    this.cancelReason = '';
  }

  confirmCancel() {
    const reason = this.cancelReason.trim();
    if (!this.cancelTarget || !reason || this.canceling) {
      return;
    }
    const order = this.cancelTarget;
    this.canceling = true;
    this.orderService.cancelOrder(order.id, reason).subscribe({
      next: (response) => {
        this.canceling = false;
        this.removeOrder(order);
        this.closeCancel();
        if (response.customerNotified) {
          this.toastr.success('Pedido cancelado e cliente avisado por e-mail.');
        } else {
          this.toastr.warning('Pedido cancelado, mas o cliente não foi avisado por e-mail.');
        }
      },
      error: (err) => {
        this.canceling = false;
        this.toastr.error(`Erro ao cancelar: ${this.backendMessage(err)}`);
      },
    });
  }

  private removeOrder(order: OrderResponse) {
    this.orders = this.orders?.filter((o) => o.id !== order.id);
    if (this.selectedOrder?.id === order.id) {
      this.selectedOrder = this.pendingOrders[0] || this.orders?.[0];
    }
  }

  // o GlobalExceptionHandler devolve { error } para regra de negócio e { message } para os demais erros
  private backendMessage(err: any): string {
    return err?.error?.error || err?.error?.message || err?.message || 'erro inesperado';
  }

  navigateEdit(id?: string) {
    this.router.navigate(['/distributor/edit', id])
  }

  getCountByStatus(status: string): number {
    return this.orders?.filter(order => order.status === status).length || 0;
  }

  getTotalRevenue(): number {
    return this.orders
      ?.filter(order => order.status !== 'Entregue')
      .reduce((sum, order) => sum + order.totalPrice, 0) || 0;
  }

}
