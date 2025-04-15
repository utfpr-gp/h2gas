import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AbstractService<T> {
  baseUrl: string = '';
  
  constructor(
    protected http: HttpClient
  ) { }


  create(data: T): Observable<T> {
    return this.http.post<T>(this.baseUrl, data)
  }

  findAll(): Observable<T[]> {
    return this.http.get<T[]>(this.baseUrl);
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
