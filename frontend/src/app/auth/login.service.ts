import {inject, Injectable, signal} from '@angular/core';
import { HttpClient } from "@angular/common/http";
import {environment} from "../../environments/environment";
import {Observable, tap} from "rxjs";
import {Router} from "@angular/router";
import {LoginResponse} from "./login-response";
import {UserResponse} from "./user-response";

@Injectable({
    providedIn: 'root'
})
export class LoginService {

    USER_NAME_SESSION_ATTRIBUTE_NAME = 'authenticatedUser';
    public username: String;
    public password: String;
    private http = inject(HttpClient);
    private router = inject(Router);

    readonly currentUser = signal<UserResponse | null>(
        JSON.parse(sessionStorage.getItem('authenticatedUser') ?? 'null')
    );

    login(username: string, password: string): Observable<LoginResponse> {
        let loginRequest: { username: string, password: string } = {
            username: username,
            password: password,
        }
        return this.http.post<LoginResponse>(environment.api_url + '/api/auth/login', loginRequest).pipe(tap(response => {
            console.log('successfully logged in');
        }));
    }

    /*
    login(username: string, password: string) {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password })
      .pipe(tap(response => {
        localStorage.setItem('token', response.token);
      }));
  }
     */

    createBasicAuthToken(username: String, password: String) {
        return 'Basic ' + window.btoa(username + ":" + password)
    }

    registerSuccessfulLogin(response: LoginResponse) {
        sessionStorage.setItem(this.USER_NAME_SESSION_ATTRIBUTE_NAME, JSON.stringify(response));
        sessionStorage.setItem('token', response.token);
        this.currentUser.set(this.getLoggedInUserName());
    }

    logout() {
        sessionStorage.removeItem(this.USER_NAME_SESSION_ATTRIBUTE_NAME);
        sessionStorage.removeItem('token');
        this.currentUser.set(null);

        this.username = null;
        this.password = null;
        this.router.navigate(['login']).then();
    }

    isUserLoggedIn() {
        let user = sessionStorage.getItem(this.USER_NAME_SESSION_ATTRIBUTE_NAME)
        if (user === null) return false
        return true
    }

    getLoggedInUserName(): UserResponse {
        let user: UserResponse = JSON.parse(sessionStorage.getItem(this.USER_NAME_SESSION_ATTRIBUTE_NAME));
        if (user === null) return null
        return user
    }

}
