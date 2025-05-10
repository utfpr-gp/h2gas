import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  baseUrl: string = `${environment.apiUrl}/auth`


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
