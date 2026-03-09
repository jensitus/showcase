import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {AuthService} from '../auth.service';
import {TranslateModule} from "@ngx-translate/core";

@Component({
    selector: 'app-reset-password',
    imports: [ReactiveFormsModule, RouterLink, TranslateModule],
    templateUrl: './reset-password.component.html',
    styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly authService = inject(AuthService);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);

    readonly loading = signal(false);
    readonly errorMessage = signal('');
    readonly successMessage = signal('');
    readonly resetComplete = signal(false);
    readonly token = signal('');

    readonly resetPasswordForm = this.fb.group({
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required]
    }, {validators: this.passwordMatchValidator});

    ngOnInit(): void {
        const tokenParam = this.route.snapshot.queryParamMap.get('token');
        if (tokenParam) {
            this.token.set(tokenParam);
        } else {
            this.errorMessage.set('Invalid or missing reset token. Please request a new password reset link.');
        }
    }

    passwordMatchValidator(form: FormGroup) {
        const newPassword = form.get('newPassword');
        const confirmPassword = form.get('confirmPassword');

        if (newPassword && confirmPassword && newPassword.value !== confirmPassword.value) {
            confirmPassword.setErrors({passwordMismatch: true});
            return {passwordMismatch: true};
        }
        return null;
    }

    onSubmit(): void {
        if (this.resetPasswordForm.invalid || !this.token()) {
            this.resetPasswordForm.markAllAsTouched();
            return;
        }

        this.loading.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        this.authService.resetPassword({
            token: this.token(),
            newPassword: this.resetPasswordForm.value.newPassword!,
            confirmPassword: this.resetPasswordForm.value.confirmPassword!
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (response) => {
                    this.loading.set(false);
                    this.resetComplete.set(true);
                    this.successMessage.set(response.message);
                },
                error: (error) => {
                    this.loading.set(false);
                    this.errorMessage.set(error.error?.error || 'Failed to reset password. Please try again.');
                }
            });
    }

    navigateToLogin(): void {
        this.router.navigate(['/login']);
    }
}
