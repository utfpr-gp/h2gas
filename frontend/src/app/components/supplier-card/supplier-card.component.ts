import { Component, EventEmitter, Inject, Input, LOCALE_ID, Output } from '@angular/core';
import { CommonModule, formatNumber } from '@angular/common';
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
  logoBroken = false;

  constructor(@Inject(LOCALE_ID) private locale: string) {}

  get hasRating(): boolean {
    return this.supplier.averageRating != null && (this.supplier.reviewCount ?? 0) > 0;
  }

  get ratingLabel(): string {
    return formatNumber(this.supplier.averageRating ?? 0, this.locale, '1.1-1');
  }

  get ratingAriaLabel(): string | null {
    if (!this.hasRating) {
      return null;
    }
    const total = this.supplier.reviewCount!;
    return `Nota ${this.ratingLabel} de 5, ${total} ${total === 1 ? 'avaliação' : 'avaliações'}`;
  }

  onViewProducts() {
    this.viewProducts.emit(this.supplier.id);
  }
}
