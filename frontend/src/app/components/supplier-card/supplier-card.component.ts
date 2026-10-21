import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Supplier } from '../../models/supplier';
import { StoreRatingComponent } from '../store-rating/store-rating.component';

@Component({
  selector: 'app-supplier-card',
  standalone: true,
  imports: [CommonModule, StoreRatingComponent],
  templateUrl: './supplier-card.component.html'
})
export class SupplierCardComponent {
  @Input() supplier!: Supplier;
  @Output() viewProducts = new EventEmitter<string>();
  logoBroken = false;

  onViewProducts() {
    this.viewProducts.emit(this.supplier.id);
  }
}
