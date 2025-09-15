import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

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

  // addProduct(product: any) {
  //   const currentCart = this.cartProductsSubject.value;
  //   const newCart = [...currentCart, product];
  //   localStorage.setItem('cart-product', JSON.stringify(newCart));
  //   this.cartProductsSubject.next(newCart);
  // }

  setCart(cart: any[]) {
    localStorage.setItem('cart-product', JSON.stringify(cart));
    this.cartProductsSubject.next(cart);
  }

  getCart() {
    return this.cartProductsSubject.value;
  }

}
