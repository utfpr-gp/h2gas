import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { CommonModule } from '@angular/common';
import { User } from '../../../models/user';
import { ToastrService } from 'ngx-toastr';
import { OrderResponse, ReorderItem } from '../../../models/order.model';
import { CartService } from '../../../services/cart.service';
import { ProductItem } from '../../../models/product-item';
import { PaginationComponent } from '../../pagination/pagination.component';
import { Router } from '@angular/router';
import { expandCollapse, staggerList } from '../../../animations/checkout.animations';

@Component({
  selector: 'app-order-list',
  imports: [
    CommonModule,
    PaginationComponent,
  ],
  templateUrl: './order-list.component.html',
  animations: [staggerList, expandCollapse],
})
export class OrderListComponent implements OnInit {

  constructor(
    private orderService: OrderService,
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router,
    private cartService: CartService,
  ) {}

  userLogado?: User;
  orders?: OrderResponse[];
  expandedIndex: number | null = null;
  reorderingId: string | null = null;

  currentPage: number = 0;
  totalPages: number = 0;
  pageSize: number = 5;
  hasNext: boolean = false;
  hasPrevious: boolean = false;

  ngOnInit(): void {
    this.userLogado = this.userService.getUserLocalStorage();

    this.getOrders();
  }

  statuses = [
    { key: 'TODOS', label: 'Todos' },
    { key: 'PENDING', label: 'Pendentes' },
    { key: 'CONFIRMED', label: 'Confirmados' },
    { key: 'IN_PREPARATION', label: 'Em preparação' },
    { key: 'OUT_FOR_DELIVERY', label: 'Saiu para entrega' },
    { key: 'DELIVERED', label: 'Entregues' },
    { key: 'CANCELED', label: 'Cancelados' }
  ];
  
  selectedStatus: string = 'TODOS';

  getOrders(page: number = 0) {
    this.orderService.findByCustomerId(this.userLogado?.id!, page, this.pageSize, this.selectedStatus).subscribe({
      next: (response) => {
        this.orders = response.data;
        this.currentPage = response.page;
        this.totalPages = response.totalPages;
        this.hasNext = response.hasNext;
        this.hasPrevious = response.hasPrevious;
        this.toastr.success("Pedidos buscados com sucesso");
      },
      error: (err) => {
        console.error(err);
        this.toastr.error(`Erro ao buscar os pedidos: ${err.error.message}`);
      },
    });
  }

  filterByStatus(statusKey: string) {
    this.selectedStatus = statusKey;
    this.getOrders(0);
  }
  
  nextPage() {
    if (this.hasNext) {
      this.getOrders(this.currentPage + 1);
    }
  }
  
  previousPage() {
    if (this.hasPrevious) {
      this.getOrders(this.currentPage - 1);
    }
  }

  toggleAccordion(index: number) {
    this.expandedIndex = this.expandedIndex === index ? null : index;
  }

  // HU05: substitui o carrinho pelo pedido refeito (dados e preços atuais) e vai direto ao checkout
  reorder(order: OrderResponse) {
    if (this.reorderingId) {
      return;
    }
    this.reorderingId = order.id;
    this.orderService.getReorderPreview(order.id).subscribe({
      next: (preview) => {
        this.reorderingId = null;
        const indisponiveis = preview.unavailableItems
          .map((item) => item.productBrand ?? 'produto removido')
          .join(', ');

        if (!preview.availableItems.length) {
          this.toastr.warning(`Nenhum item deste pedido está disponível agora: ${indisponiveis}`);
          return;
        }

        if (this.cartService.getCart().length) {
          this.toastr.info('O carrinho foi substituído pelos itens deste pedido.');
        }
        this.cartService.setCart(preview.availableItems.map((item) => this.toCartProduct(item)));

        if (preview.unavailableItems.length) {
          this.toastr.warning(`Alguns itens não puderam ser adicionados: ${indisponiveis}`);
        }
        this.router.navigate(['/resumo-pedido']);
      },
      error: (err) => {
        this.reorderingId = null;
        this.toastr.error(`Não foi possível refazer este pedido: ${err.error?.error || err.message}`);
      },
    });
  }

  private toCartProduct(item: ReorderItem): ProductItem {
    return {
      id: item.productId,
      brand: item.productBrand,
      category: item.category,
      type: item.type,
      imageUrl: item.imageUrl,
      supplierId: item.supplierId ?? '',
      priceDelivery: item.priceDelivery,
      pricePickup: item.pricePickup,
      quantitySelect: item.quantity,
    };
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
