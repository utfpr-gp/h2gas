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


}
