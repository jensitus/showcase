import {Component, computed, inject} from '@angular/core';
import {filter, map} from "rxjs";
import {NavigationEnd, Router, RouterOutlet} from "@angular/router";
import {SidebarComponent} from "./layout/layout/layout/sidebar/sidebar.component";
import {HeaderComponent} from "./layout/layout/layout/header/header.component";
import {LoginService} from "./auth/login.service";
import {toSignal} from "@angular/core/rxjs-interop";
import {LanguageService} from "./shared/language.service";

@Component({
    selector: 'app-root',
    standalone: true,
    templateUrl: './app.component.html',
    imports: [
        RouterOutlet,
        SidebarComponent,
        HeaderComponent,
    ],
    styleUrls: ['./app.component.scss']
})
export class AppComponent {

    private loginService = inject(LoginService);
    private router = inject(Router);
    // Eagerly initialize LanguageService so language preference is applied on startup
    private languageService = inject(LanguageService);

    title = 'service-b.org';
    collapedSideBar: boolean = false;

    private readonly noSidebarRoutes = ['/login', '/register', '/verify-email', '/complete-profile', '/forgot-password', '/reset-password', '/change-password'];

    private readonly currentUrl = toSignal(
        this.router.events.pipe(
            filter(e => e instanceof NavigationEnd),
            map((e: NavigationEnd) => e.urlAfterRedirects)
        ),
        {initialValue: this.router.url}
    );

    readonly showSidebar = computed(() =>
        this.loginService.currentUser() !== null &&
        !this.noSidebarRoutes.some(r => this.currentUrl().startsWith(r))
    );

    receiveCollapsed($event: boolean) {
        this.collapedSideBar = $event;
    }

}
