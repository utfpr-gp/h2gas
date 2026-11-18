import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { OrderService } from '../../../services/order.service';
import { DeliveryHistoryItem } from '../../../models/delivery-history';
import { paymentLabel } from '../../../models/payment-label';
import { listItem } from '../../../animations/checkout.animations';
import { backendMessage } from '../../../utils/backend-message';

interface DayGroup {
  label: string;
  items: DeliveryHistoryItem[];
}

// HU07 (Figura 17c): entregas concluídas e recusadas pelo entregador, agrupadas por dia
@Component({
  selector: 'app-deliverer-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './deliverer-history.component.html',
  animations: [listItem],
})
export class DelivererHistoryComponent implements OnInit {
  deliveredToday = 0;
  groups: DayGroup[] = [];
  loading = true;
  readonly paymentLabel = paymentLabel;

  constructor(
    private orderService: OrderService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.orderService.deliveryHistory().subscribe({
      next: (history) => {
        this.deliveredToday = history.deliveredToday;
        this.groups = this.groupByDay(history.items);
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao buscar o histórico: ${backendMessage(err)}`);
      },
    });
  }

  // os itens já chegam do mais recente ao mais antigo; o agrupamento só preserva essa ordem
  private groupByDay(items: DeliveryHistoryItem[]): DayGroup[] {
    const groups: DayGroup[] = [];
    for (const item of items) {
      const label = this.dayLabel(new Date(item.occurredAt));
      const last = groups[groups.length - 1];
      if (last && last.label === label) {
        last.items.push(item);
      } else {
        groups.push({ label, items: [item] });
      }
    }
    return groups;
  }

  private dayLabel(date: Date): string {
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    const sameDay = (a: Date, b: Date) => a.toDateString() === b.toDateString();
    const formatted = date.toLocaleDateString('pt-BR');
    if (sameDay(date, today)) {
      return `Hoje, ${formatted}`;
    }
    return sameDay(date, yesterday) ? `Ontem, ${formatted}` : formatted;
  }
}
