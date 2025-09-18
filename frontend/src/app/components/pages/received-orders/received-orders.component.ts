import { Component, OnInit } from '@angular/core';
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

@Component({
  selector: 'app-received-orders',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './received-orders.component.html',
  styleUrl: './received-orders.component.css'
})

export class ReceivedOrdersComponent implements OnInit {

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
    // this.getOrders();
    this.getOrderExcludeStatus();
    this.getOpenStatus();
  }

  getStatusKeys(): (keyof typeof OrderStatus)[] {
    return Object.keys(this.orderStatus) as (keyof typeof OrderStatus)[];
  }
  

  onStatusChange(event: Event) {
    const selectedValue = (event.target as HTMLSelectElement).value;

    if (selectedValue == "ALL") {
      this.getOrderExcludeStatus();
      // this.getOrders();
    } else {
      this.getOrders(selectedValue);
    }
  }

  onToggleSupplier(checked: any) {
    this.showSupplier = checked.target.checked;
    console.log('Distribuidora aberta?', checked);

    let data = {
      "status": this.showSupplier
    }

    this.supplierService.openDistribuidor(data, this.userLogado?.supplierId!).subscribe({
      next: (value) => {
        if (this.showSupplier) {
          this.toastr.success("Distribuidora aberta com sucesso!!");          
        } else {
          this.toastr.success("Distribuidora fechada com sucesso!!");          
        }
      },
      error: (err) => {
        this.toastr.error(`Erro ao abrir distribuidora: ${err.error.message} `);
      },
    })

  }


  getOrders(status?: string) {
    debugger

    this.orderService.findBySupplierId(this.userLogado?.supplierId!, status).subscribe({
      next: (response) => {
        this.orders = response.data;
        console.log(this.orders);

        this.toastr.success("Pedidos buscado com sucesso");
      },
      error: (err) => {
        console.error(err)
        this.toastr.error(`Erro ao buscar os pedidos: ${err.error.message}`);
      },
    })
  }

  getOrderExcludeStatus() {
    debugger
    let exludeStatus: string[] = ['DELIVERED', 'CANCELED'];

    this.orderService.findByExcludeStatus(this.userLogado?.supplierId!, exludeStatus).subscribe({
      next: (response) => {
        this.orders = response.data;
        console.log(this.orders);

        this.toastr.success("Pedidos buscado com sucesso");
      },
      error: (err) => {
        console.error(err)
        this.toastr.error(`Erro ao buscar os pedidos: ${err.error.message}`);
      },
    })
  }

  getOpenStatus() {
    debugger
    this.supplierService.getOpen(this.userLogado?.supplierId!).subscribe({
      next: (response) => {
        console.log('Distribuidora buscada com sucesso!', response);
        this.showSupplier = response;
        this.toastr.success('Distribuidora buscada com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        // this.toastr.error(`Erro ao buscar distribuidora: ${err.error.message}`)
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
        this.toastr.success(`Status do pedido atualizado para: ${response.status}`)        
        if (order.status === 'Cancelado' || order.status === 'Entregue') {
          this.orders = this.orders?.filter(o => o.id !== order.id);
        }
      },
      error: (err) => {
        this.toastr.error(`Erro ao salvar categoria: ${err.error.message}`)        
        console.error(err);
      },
    })
    // aqui você chama o serviço para atualizar no backend
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


  getStatusColor(status: string): string {
    switch (status) {
      case 'Pendente':
        return 'bg-gray-500 text-white';
      case 'Confirmado':
        return 'bg-blue-600 text-white';
      case 'Em preparacao':
        return 'bg-yellow-600 text-white';
      case 'Saiu para entrega':
        return 'bg-indigo-600 text-white';
      case 'Entregue':
        return 'bg-green-600 text-white';
      case 'Cancelado':
        return 'bg-red-600 text-white';
      default:
        return 'bg-gray-300 text-black';
    }
  }



}
