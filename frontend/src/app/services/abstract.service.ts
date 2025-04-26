import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
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


}
