import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';
import {UserResponse} from '../auth/user-response';

@Injectable({
    providedIn: 'root'
})
export class AdminService {

    private http = inject(HttpClient);
    private apiUrl = environment.api_url + '/api/admin';

    getUsers(): Observable<UserResponse[]> {
        return this.http.get<UserResponse[]>(`${this.apiUrl}/users`);
    }

    updateRole(id: string, role: string): Observable<UserResponse> {
        return this.http.put<UserResponse>(`${this.apiUrl}/users/${id}/role`, {role});
    }
}
