import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import {jwtDecode} from 'jwt-decode';
import { LocationContextService } from './location-context.service';
import { CartService } from './cart.service';

interface JwtPayload {
  exp: number;
  [key: string]: any;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  baseUrl: string = `${environment.apiUrl}/auth`


  constructor(
    private http: HttpClient,
    private locationContext: LocationContextService,
    private cartService: CartService,
  ) { }

  isLogged(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired();
  }

  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    try {
      const decoded = jwtDecode<JwtPayload>(token);
      const now = Math.floor(Date.now() / 1000);
      return decoded.exp < now;
    } catch (error) {
      console.error('Erro ao decodificar token:', error);
      return true;
    }
  }

  getToken() {
    return localStorage.getItem('access_token');
  }

  loginUser(data: any) {
    return this.http.post(`${this.baseUrl}/login`, data)
  }

  // HU11: login por código de 6 dígitos enviado por e-mail
  requestLoginToken(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/login/token/request`, { email });
  }

  validateLoginToken(email: string, code: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.baseUrl}/login/token/validate`, { email, code });
  }

  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user');
    this.locationContext.clear();
    // HU13: o carrinho sai junto com a sessão (e não ao entrar), para o visitante levar o carrinho ao login
    this.cartService.setCart([]);
    this.cartService.clearBuyNow();
  }

  // HU13: sessão vencida vira visitante — sai o token e o usuário, ficam o carrinho e a cidade
  dropExpiredSession() {
    if (localStorage.getItem('access_token') && this.isTokenExpired()) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('user');
    }
  }

  requestPasswordReset(data: any) {
    return this.http.post(`${environment.apiUrl}/users/reset-password`, data)
  }

  resetPassword(data: any) {
    return this.http.post(`${environment.apiUrl}/users/reset-password/confirm`, data)
  }
}
