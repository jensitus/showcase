import {Component, inject, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from "../auth.service";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-verify-email',
  standalone: true,
    imports: [
        TranslateModule
    ],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent implements OnInit {
    verifying: boolean = true;
    success: boolean = false;
    errorMessage: string = '';

    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private authService = inject(AuthService);

    ngOnInit(): void {
        this.route.queryParams.subscribe(params => {
            const token = params['token'];
            if (token) {
                this.verifyEmail(token);
            } else {
                this.verifying = false;
                this.errorMessage = 'No verification token provided.';
            }
        });
    }

    verifyEmail(token: string): void {
        this.authService.verifyEmail(token).subscribe({
            next: (response) => {
                this.verifying = false;
                this.success = true;
            },
            error: (error) => {
                this.verifying = false;
                this.success = false;
                this.errorMessage = error.error?.message || 'Email verification failed.';
            }
        });
    }

    navigateToLogin(): void {
        this.router.navigate(['/login']);
    }

    navigateToCompleteProfile(): void {
        this.router.navigate(['/login'], {queryParams: {returnTo: 'complete-profile'}});
    }

}
