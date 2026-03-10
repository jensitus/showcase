import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../environments/environment';

@Injectable({providedIn: 'root'})
export class DeploymentService {

    private http = inject(HttpClient);
    private apiUrl = `${environment.api_url}/api/deployments`;

    deploy(file: File, deploymentName: string, tenantId: string): Observable<string> {
        const form = new FormData();
        form.append('file', file);
        form.append('deployment-name', deploymentName);
        form.append('tenant-id', tenantId);
        return this.http.post(this.apiUrl, form, {responseType: 'text'});
    }
}
