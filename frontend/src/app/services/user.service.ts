import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { HttpClient } from '@angular/common/http';
import { User } from '../models/user';
import { environment } from '../../environments/environment';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService extends AbstractService<User> {
  override baseUrl: string = `${environment.apiUrl}/auth/register`;
  // override baseUrl: string = 'http://localhost:8080/h2gas/api/auth/register'

  // F7: o menu e o "Meus Dados" acompanham o usuário atual (o avatar muda quando o nome é salvo)
  private readonly currentUserSubject = new BehaviorSubject<User | null>(this.readStoredUser());
  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor(protected override http: HttpClient) {
    super(http);
  }

  setCurrentUser(user: User) {
    localStorage.setItem('user', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private readStoredUser(): User | null {
    try {
      return JSON.parse(localStorage.getItem('user') ?? 'null');
    } catch {
      return null;
    }
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

  createUser(user: any): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}`, user);
  }
}
