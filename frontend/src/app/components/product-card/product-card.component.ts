import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductItem } from '../../models/product-item';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-card.component.html'
})
export class ProductCardComponent {
  @Input() product!: ProductItem;
  @Output() addToCart = new EventEmitter<ProductItem>();

  onAdd() {
    this.addToCart.emit({ ...this.product, quantitySelect: 1 });
  }
}
