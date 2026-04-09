import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';
import {Submission} from './submission';

@Injectable({
  providedIn: 'root'
})
export class SubmissionService {

  private apiUrl = environment.api_url;

  constructor(private http: HttpClient) {}

  getMySubmissions(): Observable<Submission[]> {
    return this.http.get<Submission[]>(`${this.apiUrl}/api/submissions/my`);
  }

  createSubmission(request: {
    title: string;
    authors: string;
    abstractText: string;
    topic: string;
    submitterEmail: string;
  }): Observable<Submission> {
    return this.http.post<Submission>(`${this.apiUrl}/api/submissions`, request);
  }
}
