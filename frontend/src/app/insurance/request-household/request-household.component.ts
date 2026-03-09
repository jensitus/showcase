import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from "rxjs";
import {CustomerService} from "../../customer/customer.service";
import {RequestInsurance} from "../../customer/request-insurance";
import {Message} from "../../customer/message";
import {Customer} from "../../customer/customer";
import {FormsModule} from "@angular/forms";
import {Router} from "@angular/router";
import {Coverage} from "../coverage";
import {PaymentScheduleComponent} from "../payment-schedule/payment-schedule.component";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-request-household',
  standalone: true,
  imports: [
    FormsModule,
    PaymentScheduleComponent,
    TranslateModule
  ],
  templateUrl: './request-household.component.html',
  styleUrl: './request-household.component.scss'
})
export class RequestHouseholdComponent implements OnInit, OnDestroy {

  @Input() insuranceType: string;
  @Input() customerId: string;
  private subscription: Subscription[] = [];
  private customerService = inject(CustomerService);
  private readonly router = inject(Router);
  customer: Customer;
  flood_risk: boolean = false;
  mudslide_risk: boolean = false;
  sufficientIncome: boolean = false;
  // insuranceSumSteps: number = 1;
  insuranceSum: string = "€ 750 Tsd";
  // coverings: string[] = [Coverage.BASIS, Coverage.EXTENDED, Coverage.FULL];
  chosenCoverage: string = Coverage.BASIS;
  paymentSchedule: Map<string, number> = new Map();
  // scheduleOfPayments: ScheduleOfPayments;
  // mapScheduleOfPayments: Map<string, number> = new Map();
  // scheduleAmount: number[];

  ngOnDestroy(): void {
    this.subscription.forEach(s => s.unsubscribe());
  }

  ngOnInit(): void {
    this.getCustomer();
  }

  request_insurance() {
    let paymentSchedule: string;
    let amount: number;
    for (let [key, value] of Object.entries(this.paymentSchedule)) {
      if (key === "key") {
        paymentSchedule = value;
      } else if (key === "value") {
        amount = value;
      }
    }
    let requestInsurance: RequestInsurance = {
      insuranceType: this.insuranceType,
      customerId: this.customerId,
      mudslideRisk: this.mudslide_risk,
      floodRisk: this.flood_risk,
      sufficientIncome: this.sufficientIncome,
      insuranceCoverage: this.chosenCoverage,
      insuranceSum: this.insuranceSum,
      paymentSchedule: paymentSchedule,
      amount: amount
    }
    this.subscription.push(
      this.customerService.requestNewInsurance(requestInsurance).subscribe({
        next: value => {
          let message: Message = value;
          console.log(message.text)
          this.router.navigate(['/profile']);
        },
        error: error => console.log(error),
      })
    )
  }

  getCustomer() {
    this.subscription.push(
      this.customerService.getCustomer(this.customerId).subscribe({
        next: value => {
          this.customer = value;
        },
        error: error => console.log(error),
      })
    )
  }

  showChosenInsuranceSum(event: string) {
    this.insuranceSum = event;
  }

  showChosenPaymentSchedule(event: Map<string, number>) {
    this.paymentSchedule = event
  }

  showChosenCoverage(event: string) {
    this.chosenCoverage = event;
  }
}
