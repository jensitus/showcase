import {Component, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {AuthService} from '../auth.service';
import {TranslateModule} from "@ngx-translate/core";

@Component({
    selector: 'app-change-password',
    imports: [ReactiveFormsModule, RouterLink, TranslateModule],
    templateUrl: './change-password.component.html',
    styleUrl: './change-password.component.scss'
})
export class ChangePasswordComponent {
    private readonly fb = inject(FormBuilder);
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);

    readonly loading = signal(false);
    readonly errorMessage = signal('');
    readonly successMessage = signal('');

    readonly changePasswordForm = this.fb.group({
        currentPassword: ['', Validators.required],
        newPassword: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', Validators.required]
    }, {validators: this.passwordMatchValidator});

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
        if (this.changePasswordForm.invalid) {
            this.changePasswordForm.markAllAsTouched();
            return;
        }

        this.loading.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        this.authService.changePassword({
            currentPassword: this.changePasswordForm.value.currentPassword!,
            newPassword: this.changePasswordForm.value.newPassword!,
            confirmPassword: this.changePasswordForm.value.confirmPassword!
        }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (response) => {
                this.loading.set(false);
                this.successMessage.set(response.message);
                this.changePasswordForm.reset();
            },
            error: (error) => {
                this.loading.set(false);
                this.errorMessage.set(error.error?.error || 'Failed to change password. Please try again.');
            }
        });
    }
}
