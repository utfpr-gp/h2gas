import { Injectable } from '@angular/core';
import { supplier } from '../models/supplier';
import { AbstractService } from './abstract.service';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class SupplierService extends AbstractService<supplier> {
  override baseUrl: string = 'http://localhost:8080/h2gas/api/cities'

  constructor(protected override http: HttpClient) { 
    super(http);
  }
}
