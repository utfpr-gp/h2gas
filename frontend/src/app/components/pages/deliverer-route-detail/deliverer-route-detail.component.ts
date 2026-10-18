import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ToastrService } from 'ngx-toastr';
import { OrderService } from '../../../services/order.service';
import { OrderResponse } from '../../../models/order.model';
import { paymentLabel } from '../../../models/payment-label';
import { fadeIn } from '../../../animations/checkout.animations';

// HU07 (Figura 17b): detalhe da entrega com trajeto, contato do cliente e ações do entregador
@Component({
  selector: 'app-deliverer-route-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './deliverer-route-detail.component.html',
  animations: [fadeIn],
})
export class DelivererRouteDetailComponent implements OnInit {
  order?: OrderResponse;
  loading = true;
  busy = false;
  cancelOpen = false;
  cancelReason = '';
  mapUrl?: SafeResourceUrl;
  mapsLink = '';
  readonly paymentLabel = paymentLabel;

  constructor(
    private orderService: OrderService,
    private route: ActivatedRoute,
    private router: Router,
    private toastr: ToastrService,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    // my-routes só devolve pedidos do entregador do token; GET /orders/{id} não checa o dono
    this.orderService.myRoutes().subscribe({
      next: (routes) => {
        this.loading = false;
        const found = routes.find((r) => r.id === id);
        if (!found) {
          this.toastr.warning('Essa entrega não está mais na sua lista.');
          this.back();
          return;
        }
        this.show(found);
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar a entrega: ${this.backendMessage(err)}`);
      },
    });
  }

  get isInRoute() {
    return this.order?.status === 'Saiu para entrega';
  }

  get address() {
    const o = this.order;
    return o ? [`${o.streetName}, ${o.number}`, o.neighborhood, o.cityName].filter(Boolean).join(', ') : '';
  }

  get phoneDigits() {
    return (this.order?.customerPhone || '').replace(/\D/g, '');
  }

  get whatsappLink() {
    const digits = this.phoneDigits;
    return `https://wa.me/${digits.startsWith('55') ? digits : '55' + digits}`;
  }

  get initials() {
    return (this.order?.customerName || '?')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0].toUpperCase())
      .join('');
  }

  back() {
    this.router.navigate(['/entregas']);
  }

  start() {
    if (!this.order || this.busy) {
      return;
    }
    this.busy = true;
    this.orderService.startDelivery(this.order.id).subscribe({
      next: (updated) => {
        this.busy = false;
        this.order = { ...this.order!, status: updated.status };
        this.toastr.success('Entrega iniciada. Boa rota!');
      },
      error: (err) => {
        this.busy = false;
        this.toastr.error(`Erro ao iniciar a entrega: ${this.backendMessage(err)}`);
      },
    });
  }

  confirm() {
    if (!this.order || this.busy) {
      return;
    }
    this.busy = true;
    this.orderService.completeDelivery(this.order.id).subscribe({
      next: () => {
        this.busy = false;
        this.toastr.success('Entrega confirmada.');
        this.back();
      },
      error: (err) => {
        this.busy = false;
        this.toastr.error(`Erro ao confirmar a entrega: ${this.backendMessage(err)}`);
      },
    });
  }

  openCancel() {
    this.cancelReason = '';
    this.cancelOpen = true;
  }

  closeCancel() {
    this.cancelOpen = false;
  }

  confirmCancel() {
    const reason = this.cancelReason.trim();
    if (!this.order || !reason || this.busy) {
      return;
    }
    this.busy = true;
    this.orderService.cancelDelivery(this.order.id, reason).subscribe({
      next: () => {
        this.busy = false;
        this.cancelOpen = false;
        this.toastr.success('Entrega devolvida ao fornecedor.');
        this.back();
      },
      error: (err) => {
        this.busy = false;
        this.toastr.error(`Erro ao cancelar a entrega: ${this.backendMessage(err)}`);
      },
    });
  }

  private show(order: OrderResponse) {
    this.order = order;
    const query = encodeURIComponent(this.address);
    this.mapUrl = this.sanitizer.bypassSecurityTrustResourceUrl(`https://maps.google.com/maps?q=${query}&output=embed`);
    this.mapsLink = `https://www.google.com/maps/dir/?api=1&destination=${query}`;
  }

  // o GlobalExceptionHandler devolve { error } para regra de negócio e { message } para os demais erros
  private backendMessage(err: any): string {
    return err?.error?.error || err?.error?.message || err?.message || 'erro inesperado';
  }
}
