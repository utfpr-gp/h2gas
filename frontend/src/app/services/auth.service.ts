import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  baseUrl: string = 'http://localhost:8080/h2gas/api/auth/login'


  constructor(
    private http: HttpClient
  ) { }

  isLogged() {}

  loginUser(data: any) {
    return this.http.post(this.baseUrl, data)
  }
}
