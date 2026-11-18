import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { OrderResponse } from '../../models/order.model';

// HU28: subtotal e frete do pedido (valores do backend, RN01); a retirada não tem frete
@Component({
  selector: 'app-order-fee-lines',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-fee-lines.component.html',
})
export class OrderFeeLinesComponent {
  @Input({ required: true }) order!: OrderResponse;

  get isDelivery(): boolean {
    return this.order.deliveryMode !== 'PICKUP';
  }

  get fee(): number {
    return this.order.deliveryFee ?? 0;
  }

  get subtotal(): number {
    return this.order.subtotal ?? this.order.totalPrice - this.fee;
  }
}
