import {inject, Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AvatarService {
    private baseUrl = `${environment.api_url}/api/users`;
    private http = inject(HttpClient);

    uploadAvatar(userId: number, file: File): Observable<{ avatarUrl: string }> {
        const formData = new FormData();
        formData.append('avatar', file);
        return this.http.post<{ avatarUrl: string }>(`${this.baseUrl}/${userId}/avatar`, formData);
    }

    deleteAvatar(userId: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${userId}/avatar`);
    }

    getAvatarUrl(userId: number): string {
        return `${this.baseUrl}/${userId}/avatar`;
    }

}
