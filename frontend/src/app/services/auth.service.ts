import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import {jwtDecode} from 'jwt-decode';

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
    private http: HttpClient
  ) { }

  // isLogged() {
  //   let token = this.getToken();

  //   if (!token) {
  //     return false;
  //   } else {
  //     return true;
  //   }
  // }

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

  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user');
  }

  requestPasswordReset(data: any) {
    return this.http.post(`${this.baseUrl}/reset-password`, data)
  }
}
