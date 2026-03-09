import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import {Observable} from "rxjs";
import {Injectable} from "@angular/core";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const token = sessionStorage.getItem('token');
        const publicPaths = ['/api/auth/login', '/api/auth/register', '/api/auth/verify-email',
            '/api/auth/resend-verification', '/api/auth/forgot-password', '/api/auth/reset-password'];
        const isPublic = publicPaths.some(path => req.url.includes(path));
        if (token && !isPublic) {
            req = req.clone({
                setHeaders: {Authorization: `Bearer ${token}`}
            });
        }
        return next.handle(req);
    }

}

