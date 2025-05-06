import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  baseUrl: string = 'http://localhost:8080/h2gas/api/auth'


  constructor(
    private http: HttpClient
  ) { }

  isLogged() {
    let token = this.getToken();

    if (!token) {
      return false;
    } else {
      return true;
    }
  }

  getToken() {
    return localStorage.getItem('access_token');
  }

  loginUser(data: any) {
    return this.http.post(`${this.baseUrl}/login`, data)
  }
}
