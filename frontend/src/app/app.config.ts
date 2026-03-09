import {ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners, provideZoneChangeDetection} from '@angular/core';
import {provideRouter} from '@angular/router';

import {routes} from './app.routes';
import {HTTP_INTERCEPTORS, provideHttpClient, withFetch, withInterceptorsFromDi} from '@angular/common/http';
import {AuthInterceptor} from "./auth/auth-interceptor.service";
import {ErrorInterceptor} from "./shared/error.interceptor";
import {TranslateModule} from "@ngx-translate/core";
import {provideTranslateHttpLoader} from "@ngx-translate/http-loader";

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(routes),
        // Use DI-based interceptors to support class-style HttpInterceptors
        provideHttpClient(withFetch(), withInterceptorsFromDi()),
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
        { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
        importProvidersFrom(
            TranslateModule.forRoot({
                defaultLanguage: 'en'
            })
        ),
        ...provideTranslateHttpLoader({ prefix: '/assets/i18n/', suffix: '.json' })
    ]
};
