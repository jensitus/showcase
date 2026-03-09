import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CustomerService} from "../customer.service";
import {FormsModule} from "@angular/forms";
import {Customer} from "../customer";
import {NgbAlert, NgbDatepicker, NgbDateStruct, NgbInputDatepicker} from "@ng-bootstrap/ng-bootstrap";
import {JsonPipe, KeyValuePipe} from "@angular/common";
import {ActivatedRoute, Router} from "@angular/router";
import {Gender} from "../gender";
import {LoginService} from "../../auth/login.service";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-create-customer',
  imports: [
    FormsModule,
    NgbDatepicker,
    JsonPipe,
    NgbInputDatepicker,
    NgbAlert,
    KeyValuePipe,
    TranslateModule
  ],
  templateUrl: './create-customer.component.html',
  styleUrl: './create-customer.component.scss'
})
export class CreateCustomerComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly loginService = inject(LoginService);
  private readonly destroyRef = inject(DestroyRef);

  firstname: string;
  lastname: string;
  dateOfBirth: string;
  email: string;
  gender: string;
  street: string;
  zipCode: string;
  city: string;
  country: string;
  phoneNumber: string;
  model: NgbDateStruct;

  protected readonly Gender = Gender;

  ngOnInit(): void {
    if (!this.loginService.isUserLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    // Pre-fill user data from logged-in user
    const currentUser = this.loginService.getLoggedInUserName();
    if (currentUser) {
      this.email = currentUser.email;
      this.firstname = currentUser.firstName || '';
      this.lastname = currentUser.lastName || '';
    }
  }

  create() {
    const customer: Customer = {
      firstname: this.firstname,
      lastname: this.lastname,
      dateOfBirth: this.editDate(this.model),
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
        next: value => {
          // Check if we should return to profile or go to insurance request
          const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
          const insuranceType = this.route.snapshot.queryParamMap.get('insuranceType');

          if (returnTo === 'profile') {
            if (insuranceType) {
              // Go directly to insurance request
              this.router.navigate(['/request-insurance', value.id, insuranceType]);
            } else {
              this.router.navigate(['/profile']);
            }
          } else {
            this.router.navigate(['/customers', value.id]);
          }
        },
        error: err => {
          console.log(err);
        }
      });
  }

  editDate(dateObject: NgbDateStruct) {
    let day = '';
    let month = '';
    if (dateObject.day.toString().length === 1) {
      day = '0' + dateObject.day;
    } else {
      day = dateObject.day.toString();
    }
    if (dateObject.month.toString().length === 1) {
      month = '0' + dateObject.month;
    } else {
      month = dateObject.month.toString();
    }
    return dateObject.year + '-' + month + '-' + day;
  }
}
