import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { HttpClient } from '@angular/common/http';
import { User } from '../models/user';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService extends AbstractService<User> {
  override baseUrl: string = `${environment.apiUrl}/auth/register`;
  // override baseUrl: string = 'http://localhost:8080/h2gas/api/auth/register'

  constructor(protected override http: HttpClient) {
    super(http);
  }

  getMe() {
    return this.http.get(`${environment.apiUrl}/users/me`)
  }

  getUserLocalStorage() {
    return JSON.parse(localStorage.getItem('user')!!);    
  }

  updateUser(id: string, data: User) {
    return this.http.put(`${environment.apiUrl}/users/${id}`, data);
  }
}
