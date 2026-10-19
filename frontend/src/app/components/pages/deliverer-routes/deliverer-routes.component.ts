import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { OrderService } from '../../../services/order.service';
import { OrderResponse } from '../../../models/order.model';
import { paymentLabel } from '../../../models/payment-label';
import { listItem } from '../../../animations/checkout.animations';

// HU07 (Figura 17a): central de despacho do entregador, só com as rotas dele
@Component({
  selector: 'app-deliverer-routes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './deliverer-routes.component.html',
  animations: [listItem],
})
export class DelivererRoutesComponent implements OnInit, OnDestroy {
  routes: OrderResponse[] = [];
  loading = true;
  startingId?: string;
  readonly refreshIntervalMs = 30000;
  readonly paymentLabel = paymentLabel;
  private refreshSub?: Subscription;

  constructor(
    private orderService: OrderService,
    private toastr: ToastrService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
    // a HU08 (SSE) não cobre o módulo do entregador: despacho novo chega por atualização periódica
    this.refreshSub = interval(this.refreshIntervalMs).subscribe(() => this.load());
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  load() {
    this.orderService.myRoutes().subscribe({
      next: (routes) => {
        this.routes = routes;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao buscar suas entregas: ${this.backendMessage(err)}`);
      },
    });
  }

  // sem trackBy, cada atualização periódica recriaria todos os cards
  trackById(_: number, order: OrderResponse) {
    return order.id;
  }

  // PENDENTE = atribuído mas ainda em preparo; EM ROTA = o entregador já iniciou
  isInRoute(order: OrderResponse) {
    return order.status === 'Saiu para entrega';
  }

  open(order: OrderResponse) {
    this.router.navigate(['/entregas', order.id]);
  }

  start(order: OrderResponse) {
    if (this.startingId) {
      return;
    }
    this.startingId = order.id;
    this.orderService.startDelivery(order.id).subscribe({
      next: () => {
        this.startingId = undefined;
        this.toastr.success('Entrega iniciada. Boa rota!');
        // 3ª revisão (h): com a entrega em rota, o entregador segue no detalhe até confirmar ou cancelar
        this.open(order);
      },
      error: (err) => {
        this.startingId = undefined;
        this.toastr.error(`Erro ao iniciar a entrega: ${this.backendMessage(err)}`);
      },
    });
  }

  // o GlobalExceptionHandler devolve { error } para regra de negócio e { message } para os demais erros
  private backendMessage(err: any): string {
    return err?.error?.error || err?.error?.message || err?.message || 'erro inesperado';
  }
}
