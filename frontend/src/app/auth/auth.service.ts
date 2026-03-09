import {inject, Injectable} from '@angular/core';
import {environment} from "../../environments/environment";
import {Observable} from "rxjs";
import {UserResponse} from "./user-response";
import {HttpClient} from "@angular/common/http";
import {UserRegistrationRequest} from "./user-registration-request";

export interface ChangePasswordRequest {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
}

export interface ForgotPasswordRequest {
    email: string;
}

export interface ResetPasswordRequest {
    token: string;
    newPassword: string;
    confirmPassword: string;
}

export interface MessageResponse {
    message: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = environment.api_url + '/api/auth';

    registerUser(request: UserRegistrationRequest): Observable<UserResponse> {
        return this.http.post<UserResponse>(`${this.baseUrl}/register`, request);
    }

    verifyEmail(token: string): Observable<Map<string, string>> {
        return this.http.get<Map<string, string>>(`${this.baseUrl}/verify-email`, {
            params: {token}
        });
    }

    resendVerification(email: string): Observable<Map<string, string>> {
        return this.http.post<Map<string, string>>(`${this.baseUrl}/resend-verification`, null, {
            params: {email}
        });
    }

    changePassword(request: ChangePasswordRequest): Observable<MessageResponse> {
        return this.http.post<MessageResponse>(`${this.baseUrl}/change-password`, request);
    }

    forgotPassword(request: ForgotPasswordRequest): Observable<MessageResponse> {
        console.log("FORGOT PASSWORD", request);
        return this.http.post<MessageResponse>(`${this.baseUrl}/forgot-password`, request);
    }

    resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
        return this.http.post<MessageResponse>(`${this.baseUrl}/reset-password`, request);
    }
}
