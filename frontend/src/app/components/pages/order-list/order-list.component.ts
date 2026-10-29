import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { CommonModule } from '@angular/common';
import { User } from '../../../models/user';
import { ToastrService } from 'ngx-toastr';
import { OrderResponse, OrderReview } from '../../../models/order.model';
import { CartService } from '../../../services/cart.service';
import { OrderStatusChange, OrderTrackingService } from '../../../services/order-tracking.service';
import { PaginationComponent } from '../../pagination/pagination.component';
import { OrderReviewDialogComponent } from '../../order-review-dialog/order-review-dialog.component';
import { Router } from '@angular/router';
import { expandCollapse, staggerList } from '../../../animations/checkout.animations';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-order-list',
  imports: [
    CommonModule,
    PaginationComponent,
    OrderReviewDialogComponent,
  ],
  templateUrl: './order-list.component.html',
  animations: [staggerList, expandCollapse],
})
export class OrderListComponent implements OnInit, OnDestroy {

  constructor(
    private orderService: OrderService,
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router,
    private cartService: CartService,
    private orderTracking: OrderTrackingService,
  ) {}

  userLogado?: User;
  orders?: OrderResponse[];
  expandedIndex: number | null = null;
  reorderingId: string | null = null;
  // HU24: pedido com o diálogo de avaliação aberto
  reviewingOrder: OrderResponse | null = null;
  readonly stars = [1, 2, 3, 4, 5];
  // HU08: card que acabou de mudar de status (destaque curto, sem toast)
  highlightedOrderId: string | null = null;
  readonly highlightMs = 2000;
  private statusSub?: Subscription;
  private highlightTimer?: ReturnType<typeof setTimeout>;

  currentPage: number = 0;
  totalPages: number = 0;
  pageSize: number = 5;
  hasNext: boolean = false;
  hasPrevious: boolean = false;

  ngOnInit(): void {
    this.userLogado = this.userService.getUserLocalStorage();

    this.getOrders();

    // HU08: o status muda na tela sem refazer a busca; pedido fora da página atual é ignorado
    this.statusSub = this.orderTracking.onStatusChange.subscribe((change) => this.applyStatusChange(change));
    this.orderTracking.connect(this.userLogado!.id!);
  }

  ngOnDestroy(): void {
    this.statusSub?.unsubscribe();
    this.orderTracking.disconnect();
    clearTimeout(this.highlightTimer);
  }

  private applyStatusChange({ orderId, status }: OrderStatusChange) {
    const order = this.orders?.find((o) => o.id === orderId);
    if (!order) {
      return;
    }
    order.status = status as OrderResponse['status'];
    this.highlightedOrderId = orderId;
    clearTimeout(this.highlightTimer);
    this.highlightTimer = setTimeout(() => (this.highlightedOrderId = null), this.highlightMs);
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
      },
      error: (err) => {
        console.error(err);
        this.toastr.error(`Erro ao buscar os pedidos: ${backendMessage(err)}`);
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

        const carrinhoSubstituido = this.cartService.getCart().length > 0;
        // HU20: o carrinho guarda só ids e quantidades; o resumo busca o resto no backend
        this.cartService.setCart(
          preview.availableItems.map((item) => ({ productId: item.productId!, quantity: item.quantity })),
          preview.availableItems[0]?.supplierId ?? null,
        );

        // F7 §5.10: uma ação, um toast — o aviso de itens indisponíveis já diz que o carrinho mudou
        if (preview.unavailableItems.length) {
          this.toastr.warning(`Carrinho atualizado com este pedido, sem os itens indisponíveis: ${indisponiveis}`);
        } else if (carrinhoSubstituido) {
          this.toastr.info('O carrinho foi substituído pelos itens deste pedido.');
        }
        this.router.navigate(['/resumo-pedido']);
      },
      error: (err) => {
        this.reorderingId = null;
        this.toastr.error(`Não foi possível refazer este pedido: ${backendMessage(err)}`);
      },
    });
  }

  // HU24: pedido entregue que o cliente ainda não avaliou
  canReview(order: OrderResponse): boolean {
    return order.status === ('Entregue' as OrderResponse['status']) && !order.review;
  }

  openReview(order: OrderResponse) {
    this.reviewingOrder = order;
  }

  onReviewSaved(review: OrderReview) {
    if (this.reviewingOrder) this.reviewingOrder.review = review;
    this.reviewingOrder = null;
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
