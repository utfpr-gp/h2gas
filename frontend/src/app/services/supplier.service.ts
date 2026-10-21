import { Injectable } from '@angular/core';
import { Supplier } from '../models/supplier';
import { AbstractService } from './abstract.service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../models/paginated-response';
import { SupplierDashboard } from '../models/supplier-dashboard';
import { SupplierSettings } from '../models/supplier-settings';
import { SupplierProfile, SupplierProfileRequest } from '../models/supplier-profile';

@Injectable({
  providedIn: 'root'
})
export class SupplierService extends AbstractService<Supplier> {
  override baseUrl: string = `${environment.apiUrl}/suppliers`

  constructor(protected override http: HttpClient) {
    super(http);
  }

  // F7 fatia 6: dados e logo da loja; o backend usa o fornecedor do token
  getMyProfile(): Observable<SupplierProfile> {
    return this.http.get<SupplierProfile>(`${this.baseUrl}/mine/profile`);
  }

  updateMyProfile(request: SupplierProfileRequest): Observable<SupplierProfile> {
    return this.http.put<SupplierProfile>(`${this.baseUrl}/mine/profile`, request);
  }

  uploadMyLogo(file: File): Observable<{ logoUrl: string }> {
    const body = new FormData();
    body.append('file', file);
    return this.http.put<{ logoUrl: string }>(`${this.baseUrl}/mine/logo`, body);
  }

  removeMyLogo(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/mine/logo`);
  }

  // o backend usa o fornecedor do token
  getMySettings(): Observable<SupplierSettings> {
    return this.http.get<SupplierSettings>(`${this.baseUrl}/mine/settings`);
  }

  updateMySettings(settings: SupplierSettings): Observable<SupplierSettings> {
    return this.http.put<SupplierSettings>(`${this.baseUrl}/mine/settings`, settings);
  }

  // F7 fatia 6: active=false traz quem aguarda aprovação; filtro e busca no backend
  listForApproval(filter: { active?: boolean; q?: string; page?: number; size?: number }): Observable<PaginatedResponse<Supplier>> {
    let params = new HttpParams();
    if (filter.active != null) params = params.set('active', filter.active);
    if (filter.q?.trim()) params = params.set('q', filter.q.trim());
    if (filter.page != null) params = params.set('page', filter.page);
    if (filter.size != null) params = params.set('size', filter.size);
    return this.http.get<PaginatedResponse<Supplier>>(this.baseUrl, { params });
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


  getAllSupplierOpenAndActive(page: number = 0, size: number = 10, cityId?: number): Observable<PaginatedResponse<Supplier>> {
    const cityParam = cityId != null ? `&cityId=${cityId}` : '';
    return this.http.get<PaginatedResponse<Supplier>>(
      `${this.baseUrl}/actives/open?page=${page}&size=${size}${cityParam}`
    );
  }

  searchSuppliers(name: string): Observable<PaginatedResponse<Supplier>> {
    return this.http.get<PaginatedResponse<Supplier>>(`${this.baseUrl}/search?name=${name}`);
  }

  getActiveSupplier(supplierId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/${supplierId}/active`);
  }

  // o fornecedor vem do token; month (YYYY-MM) omitido = mês corrente
  getDashboard(month?: string): Observable<SupplierDashboard> {
    const query = month ? `?month=${month}` : '';
    return this.http.get<SupplierDashboard>(`${this.baseUrl}/mine/dashboard${query}`);
  }

  getOpen(supplierId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/${supplierId}/open`);
  }

  updateTimesOperation(data: any, id: string) {
    return this.http.patch(`${this.baseUrl}/${id}/update-times`, data);
  }

}
