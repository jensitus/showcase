import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BatchDetail, BatchListItem } from './screening.model';

@Injectable({ providedIn: 'root' })
export class ScreeningService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.api_url}/api/screening`;

  listBatches(): Observable<BatchListItem[]> {
    return this.http.get<BatchListItem[]>(`${this.apiUrl}/batches`);
  }

  getBatch(batchId: string): Observable<BatchDetail> {
    return this.http.get<BatchDetail>(`${this.apiUrl}/batches/${batchId}`);
  }
}
