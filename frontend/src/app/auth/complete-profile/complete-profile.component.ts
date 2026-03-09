import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CustomerService} from '../../customer/customer.service';
import {FormsModule} from '@angular/forms';
import {Customer} from '../../customer/customer';
import {NgbDatepicker, NgbDateStruct, NgbInputDatepicker} from '@ng-bootstrap/ng-bootstrap';
import {KeyValuePipe} from '@angular/common';
import {Router} from '@angular/router';
import {Gender} from '../../customer/gender';
import {LoginService} from '../login.service';
import {TranslateModule} from "@ngx-translate/core";

@Component({
    selector: 'app-complete-profile',
    imports: [
        FormsModule,
        NgbDatepicker,
        NgbInputDatepicker,
        KeyValuePipe,
        TranslateModule,
    ],
    templateUrl: './complete-profile.component.html',
    styleUrl: './complete-profile.component.scss'
})
export class CompleteProfileComponent implements OnInit {
    private readonly customerService = inject(CustomerService);
    private readonly router = inject(Router);
    private readonly loginService = inject(LoginService);
    private readonly destroyRef = inject(DestroyRef);

    firstname: string = '';
    lastname: string = '';
    email: string = '';
    gender: string = '';
    street: string = '';
    zipCode: string = '';
    city: string = '';
    country: string = '';
    phoneNumber: string = '';
    model: NgbDateStruct;

    errorMessage: string = '';

    protected readonly Gender = Gender;

    ngOnInit(): void {
        if (!this.loginService.isUserLoggedIn()) {
            this.router.navigate(['/login'], {queryParams: {returnTo: 'complete-profile'}});
            return;
        }

        const currentUser = this.loginService.getLoggedInUserName();
        if (currentUser) {
            this.email = currentUser.email;
            this.firstname = currentUser.firstName || '';
            this.lastname = currentUser.lastName || '';

            // Redirect to profile if customer profile already exists
            this.customerService.getCustomers()
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                    next: (customers) => {
                        const exists = customers.some(c => c.email === currentUser.email);
                        if (exists) {
                            this.router.navigate(['/profile']);
                        }
                    }
                });
        }
    }

    save(): void {
        this.errorMessage = '';
        const customer: Customer = {
            firstname: this.firstname,
            lastname: this.lastname,
            dateOfBirth: this.formatDate(this.model),
            email: this.email,
            gender: this.gender,
            street: this.street,
            zipCode: this.zipCode,
            city: this.city,
            country: this.country,
            phoneNumber: this.phoneNumber,
        };

        this.customerService.createCustomer(customer)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.router.navigate(['/profile']);
                },
                error: err => {
                    this.errorMessage = err.error?.message || 'Failed to save profile. Please try again.';
                }
            });
    }

    private formatDate(dateObject: NgbDateStruct): string {
        const day = dateObject.day.toString().padStart(2, '0');
        const month = dateObject.month.toString().padStart(2, '0');
        return `${dateObject.year}-${month}-${day}`;
    }
}
