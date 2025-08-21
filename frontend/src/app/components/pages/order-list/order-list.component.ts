import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { CommonModule } from '@angular/common';
import { User } from '../../../models/user';
import { ToastrService } from 'ngx-toastr';
import { OrderResponse } from '../../../models/order.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-order-list',
  imports: [
    CommonModule,
  ],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {

  constructor(
    private orderService: OrderService,
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router,
  ) {}

  userLogado?: User;
  orders?: OrderResponse[];
  expandedIndex: number | null = null;

  ngOnInit(): void {
    this.userLogado = this.userService.getUserLocalStorage();

    this.getOrders();
  }

  getOrders() {
    this.orderService.findByCustomerName(this.userLogado?.id!).subscribe({
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

  toggleAccordion(index: number) {
    this.expandedIndex = this.expandedIndex === index ? null : index;
  }

  navigate(route: string) {
    this.router.navigate([route])
  }

  getClasStatus(status: string) {
    switch (status) {
      case 'Pendente':
        return 'status-pendente';
      case "Confirmado":
          return 'status-confirmado';
      case "Em preparacao":
        return 'status-em-preparacao';
      case "Saiu para entrega":
        return 'status-saiu-para-entrega';
      case "Entregue":
        return 'status-entregue';
      case "Cancelado":
        return 'status-cancelado';
      case "Falhou":
        return 'status-cancelado';
      default:
        return 
    }
  }
}
