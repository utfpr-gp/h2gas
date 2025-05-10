import { Injectable } from '@angular/core';
import { City } from '../models/city';
import { HttpClient } from '@angular/common/http';
import { AbstractService } from './abstract.service';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CityService extends AbstractService<City>{
  override baseUrl: string = `${environment.apiUrl}/cities`
  // override baseUrl: string = `${environment.apiUrl}/auth/register`;


  constructor(protected override http: HttpClient) {
    super(http);
  }
}
