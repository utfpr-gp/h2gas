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
}
