import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AdminDashboard, AdminUser, AdminUserCreate, AdminUserFilter } from '../models/admin';
import { PaginatedResponse } from '../models/paginated-response';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  listUsers(filter: AdminUserFilter): Observable<PaginatedResponse<AdminUser>> {
    let params = new HttpParams();
    if (filter.type) params = params.set('type', filter.type);
    if (filter.q?.trim()) params = params.set('q', filter.q.trim());
    if (filter.page != null) params = params.set('page', filter.page);
    if (filter.size != null) params = params.set('size', filter.size);
    return this.http.get<PaginatedResponse<AdminUser>>(`${this.baseUrl}/users`, { params });
  }

  createUser(data: AdminUserCreate): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${this.baseUrl}/users`, data);
  }

  setEnabled(userId: string, enabled: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/users/${userId}/enabled`, { enabled });
  }

  dashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.baseUrl}/dashboard`);
  }
}
