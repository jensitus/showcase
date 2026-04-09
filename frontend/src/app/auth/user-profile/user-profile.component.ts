import {Component, inject, OnInit, signal} from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import {LoginService} from '../login.service';
import {UserResponse} from '../user-response';
import {DatePipe} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {AvatarComponent} from '../../shared/avatar/avatar.component';

@Component({
    selector: 'app-user-profile',
    imports: [RouterLink, DatePipe, TranslateModule, AvatarComponent],
    templateUrl: './user-profile.component.html',
    styleUrl: './user-profile.component.scss'
})
export class UserProfileComponent implements OnInit {
    private readonly loginService = inject(LoginService);
    private readonly router = inject(Router);

    readonly user = signal<UserResponse | null>(null);
    readonly currentUser = this.loginService.currentUser;

    ngOnInit(): void {
        const loggedInUser = this.loginService.getLoggedInUserName();
        if (loggedInUser) {
            this.user.set(loggedInUser);
        } else {
            this.router.navigate(['/login']);
        }
    }

    logout(): void {
        this.loginService.logout();
    }
}
