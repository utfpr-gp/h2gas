import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Supplier } from '../../models/supplier';

@Component({
  selector: 'app-supplier-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './supplier-card.component.html'
})
export class SupplierCardComponent {
  @Input() supplier!: Supplier;
  @Output() viewProducts = new EventEmitter<string>();

  onViewProducts() {
    this.viewProducts.emit(this.supplier.id);
  }
}
