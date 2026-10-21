import { Component, EventEmitter, Output } from '@angular/core';

// HU14: diálogo ao tentar adicionar produto de outra loja
@Component({
  selector: 'app-cart-other-store-dialog',
  standalone: true,
  imports: [],
  templateUrl: './cart-other-store-dialog.component.html',
})
export class CartOtherStoreDialogComponent {
  @Output() clearAndAdd = new EventEmitter<void>();
  @Output() keepCart = new EventEmitter<void>();
}
