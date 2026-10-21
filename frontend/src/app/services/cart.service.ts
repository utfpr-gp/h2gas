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

  updateQuantity(productId: number, quantity: number) {
    const cart = this.cartProductsSubject.value.map(item =>
      item.id === productId ? { ...item, quantitySelect: Math.max(1, quantity) } : item
    );
    this.saveToLocalStorage(cart);
  }

  removeProduct(productId: number) {
    this.saveToLocalStorage(this.cartProductsSubject.value.filter(item => item.id !== productId));
  }

  // F7 §5.23: "Comprar agora" — um item avulso na sessão, separado do carrinho (que nunca é tocado)
  private readonly BUY_NOW_KEY = 'buy-now-item';

  startBuyNow(product: ProductItem) {
    sessionStorage.setItem(this.BUY_NOW_KEY, JSON.stringify({ ...product, quantitySelect: 1 }));
  }

  getBuyNow(): ProductItem | null {
    try {
      const data = sessionStorage.getItem(this.BUY_NOW_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }

  updateBuyNowQuantity(quantity: number) {
    const item = this.getBuyNow();
    if (item) {
      sessionStorage.setItem(this.BUY_NOW_KEY, JSON.stringify({ ...item, quantitySelect: Math.max(1, quantity) }));
    }
  }

  clearBuyNow() {
    sessionStorage.removeItem(this.BUY_NOW_KEY);
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
