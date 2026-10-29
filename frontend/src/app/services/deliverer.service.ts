import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Deliverer, DelivererCreate } from '../models/deliverer';

@Injectable({
  providedIn: 'root'
})
export class DelivererService {
  private baseUrl = `${environment.apiUrl}/deliverers`;

  constructor(private http: HttpClient) {}

  // o fornecedor é o usuário logado; não existe parâmetro de fornecedor
  listMine(): Observable<Deliverer[]> {
    return this.http.get<Deliverer[]>(`${this.baseUrl}/mine`);
  }

  create(data: DelivererCreate): Observable<Deliverer> {
    return this.http.post<Deliverer>(this.baseUrl, data);
  }

  // HU22: senha nova para um entregador da própria frota (a atual não é recuperável)
  resetPassword(id: number, password: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}/password`, { password });
  }
}
