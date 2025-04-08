import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { HttpClient } from '@angular/common/http';
import { User } from '../models/user';

@Injectable({
  providedIn: 'root'
})
export class UserService extends AbstractService<User> {
  override baseUrl: string = 'http://localhost:8080/h2gas/api/users'

  constructor(protected override http: HttpClient) {
    super(http);
  }

  getMe() {
    return this.http.get(`${this.baseUrl}/me`)
  }

  getUserLocalStorage() {
    return JSON.parse(localStorage.getItem('user')!!);    
  }

}
