import {Component, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {AuthService} from '../auth.service';
import {TranslateModule} from "@ngx-translate/core";

@Component({
    selector: 'app-forgot-password',
    imports: [ReactiveFormsModule, RouterLink, TranslateModule],
    templateUrl: './forgot-password.component.html',
    styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
    private readonly fb = inject(FormBuilder);
    private readonly authService = inject(AuthService);
    private readonly destroyRef = inject(DestroyRef);

    readonly loading = signal(false);
    readonly errorMessage = signal('');
    readonly successMessage = signal('');
    readonly emailSent = signal(false);

    readonly forgotPasswordForm = this.fb.group({
        email: ['', [Validators.required, Validators.email]]
    });

    onSubmit(): void {
        if (this.forgotPasswordForm.invalid) {
            this.forgotPasswordForm.markAllAsTouched();
            return;
        }

        this.loading.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        this.authService.forgotPassword({email: this.forgotPasswordForm.value.email!})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (response) => {
                    this.loading.set(false);
                    this.emailSent.set(true);
                    this.successMessage.set(response.message);
                },
                error: (error) => {
                    this.loading.set(false);
                    this.errorMessage.set(error.error?.message || 'An error occurred. Please try again.');
                }
            });
    }
}
