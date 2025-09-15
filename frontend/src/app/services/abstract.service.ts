import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PaginatedResponse } from '../models/paginated-response';
// import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AbstractService<T> {
  baseUrl: string = '';
  
  constructor(
    protected http: HttpClient
  ) {
    this.baseUrl = environment.apiUrl;
  }


  create(data: T): Observable<T> {
    return this.http.post<T>(this.baseUrl, data)
  }

  findAll(): Observable<T[]> {
    return this.http.get<T[]>(this.baseUrl);
  }

  findAllPaginated(): Observable<PaginatedResponse<T>> {
    return this.http.get<PaginatedResponse<T>>(this.baseUrl);
  }
  

  findById(id: number | string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}/${id}`);
  }

  update(id: number | string, data: Partial<T>): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

}
