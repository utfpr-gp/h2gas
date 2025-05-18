import { Injectable } from '@angular/core';
import { AbstractService } from './abstract.service';
import { Address } from '../models/address';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AddressService extends AbstractService<Address>{
  override baseUrl: string = `${environment.apiUrl}/addresses`


  constructor(protected override http: HttpClient) { 
    super(http);
  }

  buscaViaCep(cep: string) {
    return this.http.get(`https://viacep.com.br/ws/${cep}/json/`);
  }

  findByUserId(userId: string) {
    return this.http.get(`${this.baseUrl}/by-user/${userId}`);
  }
}
