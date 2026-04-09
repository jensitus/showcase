import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import {catchError, Observable, throwError} from "rxjs";
import {inject, Injectable} from "@angular/core";
import {LoginService} from "../auth/login.service";

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  private loginService = inject(LoginService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(catchError(err => {
      if (err.status === 401) {
        this.loginService.logout();
      }
      return throwError(err);
    }));
  }
}
