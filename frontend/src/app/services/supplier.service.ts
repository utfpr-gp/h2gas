import { Injectable } from '@angular/core';
import { Supplier } from '../models/supplier';
import { AbstractService } from './abstract.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SupplierService extends AbstractService<Supplier> {
  override baseUrl: string = `${environment.apiUrl}/suppliers`

  constructor(protected override http: HttpClient) { 
    super(http);
  }

  activeSuppliers(data: any, id: number) {
    return this.http.patch(`${this.baseUrl}/${id}/update-active`, data);
  }
}
