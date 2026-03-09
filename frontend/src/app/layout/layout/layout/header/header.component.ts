import {Component, inject} from '@angular/core';
import {LoginService} from "../../../../auth/login.service";
import {NavigationEnd, Router, RouterLink} from "@angular/router";
import {AvatarComponent} from "../../../../shared/avatar/avatar.component";
import {TranslateModule} from "@ngx-translate/core";
import {LanguageService} from "../../../../shared/language.service";

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    imports: [
        AvatarComponent,
        RouterLink,
        TranslateModule
    ],
    styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
    public pushRightClass: string = 'push-right';

    private loginService = inject(LoginService);
    private router = inject(Router);
    readonly languageService = inject(LanguageService);

    readonly currentUser = this.loginService.currentUser;

    constructor() {
        this.router.events.subscribe(val => {
            if (
                val instanceof NavigationEnd &&
                window.innerWidth <= 992 &&
                this.isToggled()
            ) {
                this.toggleSidebar();
            }
        });
    }

    isToggled(): boolean {
        const dom: Element = document.querySelector('body');
        return dom.classList.contains(this.pushRightClass);
    }

    toggleSidebar() {
        const dom: any = document.querySelector('body');
        dom.classList.toggle(this.pushRightClass);
    }

    rltAndLtr() {
        const dom: any = document.querySelector('body');
        dom.classList.toggle('rtl');
    }
}
