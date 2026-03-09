import {Component, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {AuthService} from "../auth.service";
import {UserRegistrationRequest} from "../user-registration-request";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslateModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
    private readonly fb = inject(FormBuilder);
    private readonly router = inject(Router);
    private readonly authService = inject(AuthService);
    private readonly destroyRef = inject(DestroyRef);

    readonly errorMessage = signal('');
    readonly successMessage = signal('');
    readonly loading = signal(false);
    readonly registrationComplete = signal(false);

    readonly registerForm = this.fb.group({
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
        firstName: [''],
        lastName: ['']
    }, { validators: this.passwordMatchValidator });

    passwordMatchValidator(form: FormGroup) {
        const password = form.get('password');
        const confirmPassword = form.get('confirmPassword');

        if (password && confirmPassword && password.value !== confirmPassword.value) {
            confirmPassword.setErrors({ passwordMismatch: true });
            return { passwordMismatch: true };
        }
        return null;
    }

    onSubmit(): void {
        if (this.registerForm.invalid) {
            Object.keys(this.registerForm.controls).forEach(key => {
                this.registerForm.get(key)?.markAsTouched();
            });
            return;
        }

        this.loading.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        const request: UserRegistrationRequest = {
            username: this.registerForm.value.username!,
            email: this.registerForm.value.email!,
            password: this.registerForm.value.password!,
            firstName: this.registerForm.value.firstName,
            lastName: this.registerForm.value.lastName
        };

        this.authService.registerUser(request)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.loading.set(false);
                    this.registrationComplete.set(true);
                    this.successMessage.set('Registration successful! Please check your email to verify your account.');
                    this.registerForm.reset();
                },
                error: (error) => {
                    this.loading.set(false);
                    this.errorMessage.set(error.error?.message || 'Registration failed. Please try again.');
                }
            });
    }

    resendVerificationEmail(): void {
        const email = this.registerForm.value.email;
        if (!email) {
            this.errorMessage.set('Please enter your email address');
            return;
        }

        this.loading.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        this.authService.resendVerification(email)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.loading.set(false);
                    this.successMessage.set('Verification email sent! Please check your inbox.');
                },
                error: (error) => {
                    this.loading.set(false);
                    this.errorMessage.set(error.error?.message || 'Failed to resend verification email.');
                }
            });
    }

    navigateToLogin(): void {
        this.router.navigate(['/login']);
    }
}
