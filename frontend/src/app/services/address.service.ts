import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { Address } from '../models/address';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class AddressService extends AbstractService<Address>{
  override baseUrl: string = 'http://localhost:8080/h2gas/api/cities'

  constructor(protected override http: HttpClient) { 
    super(http);
  }
}
