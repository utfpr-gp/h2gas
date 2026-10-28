import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CartLine, CartPreview } from '../models/cart';
import { DeliveryMode } from '../models/order.model';

// HU20: dados, preço (RN01) e disponibilidade atuais do carrinho — rota pública, vale para o visitante
@Injectable({ providedIn: 'root' })
export class CartApiService {
  private readonly baseUrl = `${environment.apiUrl}/cart`;

  constructor(private http: HttpClient) {}

  preview(items: CartLine[], deliveryMode: DeliveryMode): Observable<CartPreview> {
    return this.http.post<CartPreview>(`${this.baseUrl}/preview`, { items, deliveryMode });
  }
}
