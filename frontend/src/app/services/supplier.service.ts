import { Injectable } from '@angular/core';
import { Supplier } from '../models/supplier';
import { AbstractService } from './abstract.service';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class SupplierService extends AbstractService<Supplier> {
  override baseUrl: string = 'http://localhost:8080/h2gas/api/suppliers'

  constructor(protected override http: HttpClient) { 
    super(http);
  }
}
