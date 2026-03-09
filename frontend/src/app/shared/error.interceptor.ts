import { HttpEvent, HttpHandler, HttpInterceptor, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import {catchError, Observable, throwError} from "rxjs";
import {inject, Injectable} from "@angular/core";
import {Router} from "@angular/router";

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  private router = inject(Router);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(catchError(err =>  {
      if (err.status === 401) {
        this.router.navigate(['login']).then();
      }
      return throwError(err);
    }));
  }
}
