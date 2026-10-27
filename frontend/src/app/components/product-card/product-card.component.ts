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
  // HU18: com motivo (ex.: loja fechada), o produto aparece mas não pode ser comprado
  @Input() blockedReason: string | null = null;
  @Output() addToCart = new EventEmitter<ProductItem>();

  @Output() buyNow = new EventEmitter<ProductItem>();

  onBuyNow() {
    if (this.blockedReason) return;
    this.buyNow.emit({ ...this.product, quantitySelect: 1 });
  }

  onAdd() {
    if (this.blockedReason) return;
    this.addToCart.emit({ ...this.product, quantitySelect: 1 });
  }
}
