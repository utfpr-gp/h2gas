import { Component } from '@angular/core';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';
import { SupplierService } from '../../../services/supplier.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { OrdersWidgetComponent } from '../../orders-widget/orders-widget.component';
import { OrderResponse } from '../../../models/order.model';
import { OrderStatus } from '../../../models/order-status.enum';
import { User } from '../../../models/user';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-order-history',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    OrdersWidgetComponent
  ],
  templateUrl: './order-history.component.html',
  styleUrl: './order-history.component.css'
})
export class OrderHistoryComponent {

  constructor(
    private orderService: OrderService,
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router,
    private supplierService: SupplierService,
  ) { }

  userLogado?: User;
  orders?: OrderResponse[];
  orderStatus = OrderStatus;
  showSupplier = false;

  actionsByStatus: Record<OrderStatus, { label: string, icon: string, color: string, action: string }[]> = {
    [OrderStatus.PENDING]: [
      { label: 'Confirmar', icon: 'fa-check', color: 'bg-blue-600 hover:bg-blue-700', action: 'CONFIRMED' },
      { label: 'Cancelar', icon: 'fa-xmark', color: 'bg-red-600 hover:bg-red-700', action: 'cancel' }
    ],
    [OrderStatus.CONFIRMED]: [
      { label: 'Preparar', icon: 'fa-box', color: 'bg-yellow-600 hover:bg-yellow-700', action: 'IN_PREPARATION' },
      { label: 'Cancelar', icon: 'fa-xmark', color: 'bg-red-600 hover:bg-red-700', action: 'cancel' }
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
    this.getOrderExcludeStatus();
    this.getOpenStatus();
  }
  

  onStatusChange(status: string) {
    if (status == "ALL") {
      this.getOrderExcludeStatus();
    } else {
      this.getOrders(status);
    }
  }

  onToggleSupplier(checked: any) {
    this.showSupplier = checked.target.checked;

    let data = {
      "status": this.showSupplier
    }

    this.supplierService.openDistribuidor(data, this.userLogado?.supplierId!).subscribe({
      next: (value) => {
        if (this.showSupplier) {
          this.toastr.success('Distribuidora aberta.');          
        } else {
          this.toastr.success('Distribuidora fechada.');          
        }
      },
      error: (err) => {
        this.toastr.error(`Erro ao abrir a distribuidora: ${backendMessage(err)}`);
      },
    })

  }


  getOrders(status?: string) {
    this.orderService.findBySupplierId(this.userLogado?.supplierId!, status).subscribe({
      next: (response) => {
        this.orders = response.data;
        console.log(this.orders);

      },
      error: (err) => {
        console.error(err)
        this.toastr.error(`Erro ao buscar os pedidos: ${backendMessage(err)}`);
      },
    })
  }

  getOrderExcludeStatus() {
    let exludeStatus: string[] = ['PENDING', 'CONFIRMED', 'IN_PREPARATION', 'OUT_FOR_DELIVERY'];

    this.orderService.findByExcludeStatus(this.userLogado?.supplierId!, exludeStatus).subscribe({
      next: (response) => {
        this.orders = response.data;
        console.log(this.orders);

      },
      error: (err) => {
        console.error(err)
        this.toastr.error(`Erro ao buscar os pedidos: ${backendMessage(err)}`);
      },
    })
  }

  getOpenStatus() {
    
    this.supplierService.getOpen(this.userLogado?.supplierId!).subscribe({
      next: (response) => {
        console.log('Distribuidora buscada com sucesso!', response);
        this.showSupplier = response;
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
      }
    })
  }

  changeStatus(order: OrderResponse, action: string) {
    console.log('Executando ação', action, 'para o pedido', order.id);
    let data = {
      "orderStatus": action
    }


    this.orderService.updateStatusOrder(data, order.id).subscribe({
      next: (response: any) => {
        order.status = response.status
        this.toastr.success(`Pedido atualizado para: ${response.status}`)        
        if (order.status === 'Cancelado' || order.status === 'Entregue') {
          this.orders = this.orders?.filter(o => o.id !== order.id);
        }
      },
      error: (err) => {
        this.toastr.error(`Erro ao atualizar o pedido: ${backendMessage(err)}`)        
        console.error(err);
      },
    })
  }

  navigateEdit(id?: number) {
    this.router.navigate(['/distributor/edit', id])
  }

  getCountByStatus(status: string): number {
    return this.orders?.filter(order => order.status === status).length || 0;
  }

  getTotalRevenue(): number {
    return this.orders
      ?.filter(order => order.status === 'Entregue')
      .reduce((sum, order) => sum + order.totalPrice, 0) || 0;
  }
}
