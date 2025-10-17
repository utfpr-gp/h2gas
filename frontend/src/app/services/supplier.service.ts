import { Injectable } from '@angular/core';
import { Supplier } from '../models/supplier';
import { AbstractService } from './abstract.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../models/paginated-response';

@Injectable({
  providedIn: 'root'
})
export class SupplierService extends AbstractService<Supplier> {
  override baseUrl: string = `${environment.apiUrl}/suppliers`

  constructor(protected override http: HttpClient) { 
    super(http);
  }

  activeSuppliers(data: any, id: string) {
    return this.http.patch(`${this.baseUrl}/${id}/update-active`, data);
  }

  findByUserId(userId: string) {
    return this.http.get(`${this.baseUrl}/by-user/${userId}`);
  }

  openDistribuidor(data: any, id: string) {
    return this.http.patch(`${this.baseUrl}/${id}/status`, data);
  }

  getAllSupplierOpenAndActive(): Observable<PaginatedResponse<Supplier>> {
    return this.http.get<PaginatedResponse<Supplier>>(`${this.baseUrl}/actives/open`);
  }

  searchSuppliers(name: string): Observable<PaginatedResponse<Supplier>> {
    return this.http.get<PaginatedResponse<Supplier>>(`${this.baseUrl}/search?name=${name}`);
  }

  getActiveSupplier(supplierId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/${supplierId}/active`);
  }

  getOpen(supplierId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/${supplierId}/open`);
  }
  
}
