import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { OrderResponse } from '../../models/order.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderStatus } from '../../models/order-status.enum';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-orders-widget',
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './orders-widget.component.html',
  styleUrl: './orders-widget.component.css'
})
export class OrdersWidgetComponent implements OnInit {
  @Input() orders: OrderResponse[] = [];
  @Input() orderStatus = OrderStatus;
  @Input() actionsByStatus: Record<string, { label: string, icon: string, color: string, action: string }[]> = {};
  @Input() showSupplierToggle = false;
  @Input() showSupplier = false;

  @Output() statusChange = new EventEmitter<string>();
  @Output() toggleSupplier = new EventEmitter<boolean>();
  @Output() changeOrderStatus = new EventEmitter<{ order: OrderResponse, action: string }>();

  constructor(private route: ActivatedRoute) {}

  rotaAtiva: string = '';

  ngOnInit(): void {
    debugger
    this.rotaAtiva = this.route.snapshot.url.map(segment => segment.path).join('/')
  }


  getStatusKeys(): (keyof typeof OrderStatus)[] {
    return Object.keys(this.orderStatus) as (keyof typeof OrderStatus)[];
  }

  onStatusChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.statusChange.emit(value);
  }

  onToggleSupplierChange(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.toggleSupplier.emit(checked);
  }

  onChangeOrderStatus(order: OrderResponse, action: string) {
    this.changeOrderStatus.emit({ order, action });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'Pendente': return 'bg-gray-500 text-white';
      case 'Confirmado': return 'bg-blue-600 text-white';
      case 'Em preparacao': return 'bg-yellow-600 text-white';
      case 'Saiu para entrega': return 'bg-indigo-600 text-white';
      case 'Entregue': return 'bg-green-600 text-white';
      case 'Cancelado': return 'bg-red-600 text-white';
      default: return 'bg-gray-300 text-black';
    }
  }
}
