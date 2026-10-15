import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ProductItem } from '../models/product-item';
import { DeliveryMode } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class CartService {

  constructor() { }

  private cartProductsSubject = new BehaviorSubject<any[]>(this.getFromLocalStorage());
  cartProducts$ = this.cartProductsSubject.asObservable();

  private getFromLocalStorage(): any[] {
    const data = localStorage.getItem('cart-product');
    return data ? JSON.parse(data) : [];
  }

  private deliveryModeSubject = new BehaviorSubject<DeliveryMode>(this.readDeliveryModeFromStorage());
  deliveryMode$ = this.deliveryModeSubject.asObservable();

  private readDeliveryModeFromStorage(): DeliveryMode {
    return localStorage.getItem('cart-delivery-mode') === 'PICKUP' ? 'PICKUP' : 'DELIVERY';
  }

  getDeliveryMode(): DeliveryMode {
    return this.deliveryModeSubject.value;
  }

  setDeliveryMode(mode: DeliveryMode) {
    localStorage.setItem('cart-delivery-mode', mode);
    this.deliveryModeSubject.next(mode);
  }

  setCart(cart: any[]) {
    localStorage.setItem('cart-product', JSON.stringify(cart));
    this.cartProductsSubject.next(cart);
  }

  getCart() {
    return this.cartProductsSubject.value;
  }

  private saveToLocalStorage(cart: ProductItem[]) {
    localStorage.setItem('cart-product', JSON.stringify(cart));
    this.cartProductsSubject.next(cart);
  }

  addProduct(product: any) {
    const currentCart = this.cartProductsSubject.value;
    const existing = currentCart.find(p => p.id === product.id);

    if (existing) {
      existing.quantitySelect = (existing.quantitySelect ?? 0) + (product.quantitySelect ?? 1);
    } else {
      currentCart.push({ ...product });
    }

    this.saveToLocalStorage(currentCart);
  }

}
