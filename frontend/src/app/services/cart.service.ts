import { Injectable } from '@angular/core';
import { BehaviorSubject, map } from 'rxjs';
import { DeliveryMode } from '../models/order.model';
import { BuyNowLine, CartLine } from '../models/cart';

// o que os cards passam ao adicionar: só id, loja e quantidade são lidos
export interface CartProductInput {
  id?: number;
  supplierId?: string | null;
  quantitySelect?: number;
}

const CART_KEY = 'cart-product';
const SUPPLIER_KEY = 'cart-supplier';
const BUY_NOW_KEY = 'buy-now-item';

// HU20: aceita o formato novo e o antigo (produto inteiro com id + quantitySelect)
function toLine(raw: any): CartLine | null {
  const productId = Number(raw?.productId ?? raw?.id);
  if (!Number.isFinite(productId) || productId <= 0) {
    return null;
  }
  const quantity = Math.max(1, Number(raw?.quantity ?? raw?.quantitySelect ?? 1) || 1);
  return { productId, quantity };
}

@Injectable({
  providedIn: 'root'
})
export class CartService {

  // HU20: no navegador, só id + quantidade e a loja do carrinho (HU14)
  private cartSubject = new BehaviorSubject<CartLine[]>([]);
  cartProducts$ = this.cartSubject.asObservable();
  count$ = this.cartProducts$.pipe(map((lines) => lines.reduce((sum, line) => sum + line.quantity, 0)));
  private supplierId: string | null = null;

  constructor() {
    this.loadFromStorage();
  }

  private loadFromStorage() {
    let raw: any[] = [];
    try {
      const parsed = JSON.parse(localStorage.getItem(CART_KEY) ?? '[]');
      raw = Array.isArray(parsed) ? parsed : [];
    } catch {
      raw = [];
    }
    const lines = raw.map(toLine).filter((line): line is CartLine => !!line);
    const legacySupplier = raw.find((item) => item?.supplierId)?.supplierId;
    const supplierId = localStorage.getItem(SUPPLIER_KEY) ?? (legacySupplier ? String(legacySupplier) : null);
    // regrava uma vez: o carrinho antigo sai do navegador já convertido
    this.save(lines, supplierId);
  }

  private save(lines: CartLine[], supplierId: string | null) {
    this.supplierId = lines.length ? supplierId : null;
    localStorage.setItem(CART_KEY, JSON.stringify(lines));
    if (this.supplierId) {
      localStorage.setItem(SUPPLIER_KEY, this.supplierId);
    } else {
      localStorage.removeItem(SUPPLIER_KEY);
    }
    this.cartSubject.next(lines);
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

  getCart(): CartLine[] {
    return this.cartSubject.value;
  }

  setCart(lines: CartLine[], supplierId: string | null) {
    this.save(lines.map(toLine).filter((line): line is CartLine => !!line), supplierId);
  }

  clear() {
    this.save([], null);
  }

  updateQuantity(productId: number, quantity: number) {
    this.save(this.getCart().map((line) =>
      line.productId === productId ? { ...line, quantity: Math.max(1, quantity) } : line
    ), this.supplierId);
  }

  removeProduct(productId: number) {
    this.save(this.getCart().filter((line) => line.productId !== productId), this.supplierId);
  }

  // F7 §5.23: "Comprar agora" — um item avulso na sessão, separado do carrinho (que nunca é tocado)
  startBuyNow(product: CartProductInput) {
    const item: BuyNowLine = { productId: product.id!, quantity: 1, supplierId: product.supplierId ? String(product.supplierId) : null };
    sessionStorage.setItem(BUY_NOW_KEY, JSON.stringify(item));
  }

  getBuyNow(): BuyNowLine | null {
    try {
      const raw = JSON.parse(sessionStorage.getItem(BUY_NOW_KEY) ?? 'null');
      const line = toLine(raw);
      return line ? { ...line, supplierId: raw.supplierId ? String(raw.supplierId) : null } : null;
    } catch {
      return null;
    }
  }

  updateBuyNowQuantity(quantity: number) {
    const item = this.getBuyNow();
    if (item) {
      sessionStorage.setItem(BUY_NOW_KEY, JSON.stringify({ ...item, quantity: Math.max(1, quantity) }));
    }
  }

  clearBuyNow() {
    sessionStorage.removeItem(BUY_NOW_KEY);
  }

  // HU14: loja dona do carrinho; vazio → null
  getCartSupplierId(): string | null {
    return this.getCart().length ? this.supplierId : null;
  }

  // HU14: 'added' se entrou/somou; 'other_supplier' se outra loja (carrinho intacto)
  addProduct(product: CartProductInput): 'added' | 'other_supplier' {
    const cartSupplierId = this.getCartSupplierId();
    const productSupplierId = product?.supplierId ? String(product.supplierId) : '';
    if (cartSupplierId && productSupplierId !== cartSupplierId) {
      return 'other_supplier';
    }

    const quantity = Math.max(1, product.quantitySelect ?? 1);
    const lines = [...this.getCart()];
    const index = lines.findIndex((line) => line.productId === product.id);
    if (index >= 0) {
      lines[index] = { ...lines[index], quantity: lines[index].quantity + quantity };
    } else {
      lines.push({ productId: product.id!, quantity });
    }
    this.save(lines, cartSupplierId ?? (productSupplierId || null));
    return 'added';
  }

  // HU14: esvazia e coloca o produto da outra loja
  clearAndAdd(product: CartProductInput): void {
    this.clear();
    this.addProduct(product);
  }
}
