import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderStatus } from '../../models/order-status.enum';
import { DeliveryMode } from '../../models/order.model';

type StepState = 'done' | 'current' | 'todo';

// HU06 (Figura 14): linha do tempo de status do pedido no painel do fornecedor
@Component({
  selector: 'app-order-status-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-status-timeline.component.html',
})
export class OrderStatusTimelineComponent {
  @Input() status?: string;
  @Input() deliveryMode?: DeliveryMode;

  private readonly order: string[] = [
    OrderStatus.PENDING,
    OrderStatus.CONFIRMED,
    OrderStatus.IN_PREPARATION,
    OrderStatus.OUT_FOR_DELIVERY,
    OrderStatus.DELIVERED,
  ];

  get canceled(): boolean {
    return this.status === OrderStatus.CANCELED;
  }

  get steps(): { label: string; icon: string; state: StepState }[] {
    const currentIndex = this.order.indexOf(this.status ?? '');
    const labels = [
      { label: 'Pendente', icon: 'schedule' },
      { label: 'Confirmado', icon: 'thumb_up' },
      { label: 'Em preparo', icon: 'inventory_2' },
      this.deliveryMode === 'PICKUP'
        ? { label: 'Pronto para retirada', icon: 'storefront' }
        : { label: 'Saiu para entrega', icon: 'local_shipping' },
      { label: 'Entregue', icon: 'task_alt' },
    ];
    return labels.map((step, i) => ({
      ...step,
      state: i < currentIndex ? 'done' : i === currentIndex ? 'current' : 'todo',
    }));
  }
}
