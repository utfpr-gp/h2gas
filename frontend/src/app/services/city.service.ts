import { Injectable } from '@angular/core';
import { City } from '../models/city';
import { HttpClient } from '@angular/common/http';
import { AbstractService } from './abstract.service';

@Injectable({
  providedIn: 'root'
})
export class CityService extends AbstractService<City>{
  override baseUrl: string = 'http://localhost:8080/h2gas/api/addresses'

  constructor(protected override http: HttpClient) {
    super(http);
  }
}
