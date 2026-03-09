import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from "rxjs";
import {CustomerService} from "../../customer/customer.service";
import {Customer} from "../../customer/customer";
// removed FontAwesome imports; using Bootstrap Icons in templates where needed
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {RequestInsurance} from "../../customer/request-insurance";
import {Message} from "../../customer/message";
import {Router} from "@angular/router";
import {CustomerCardComponent} from "../customer-card/customer-card.component";
import {PaymentScheduleComponent} from "../payment-schedule/payment-schedule.component";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-request-liability',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    CustomerCardComponent,
    PaymentScheduleComponent,
    TranslateModule
  ],
  templateUrl: './request-liability.component.html',
  styleUrl: './request-liability.component.scss'
})
export class RequestLiabilityComponent implements OnInit, OnDestroy {

  @Input() insuranceType: string;
  @Input() customerId: string;
  private subscription: Subscription[] = [];
  private customerService = inject(CustomerService);
  private readonly router = inject(Router);
  customer: Customer;
  paymentSchedule: Map<string, number> = new Map();

  ngOnDestroy(): void {
    this.subscription.forEach(s => s.unsubscribe());
  }

  ngOnInit(): void {
    this.getCustomer();
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

  sufficientIncome: boolean;
  mudslide_risk: boolean = false;
  flood_risk: boolean = false;
  insuranceSumSteps: number = 1;
  insuranceSum: string = "€ 750 Tsd";
  chosenCoverage: string;
  chosenPS: string;
  chosenAmount: number;

  request_insurance() {
    let requestInsurance: RequestInsurance = {
      insuranceType: this.insuranceType,
      customerId: this.customerId,
      mudslideRisk: this.mudslide_risk,
      floodRisk: this.flood_risk,
      sufficientIncome: this.sufficientIncome,
      insuranceCoverage: this.chosenCoverage,
      insuranceSum: this.insuranceSum,
      paymentSchedule: this.chosenPS,
      amount: this.chosenAmount,
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

  showChosenInsuranceSum(event: string) {
    this.insuranceSum = event;
  }

  showChosenPaymentSchedule(event: Map<string, number>) {
    this.paymentSchedule = event
    console.log('this.paymentSchedule', this.paymentSchedule);
  }

  showChosenCoverage(event: string) {
    this.chosenCoverage = event;
  }

  showChosenPS(event: string) {
    this.chosenPS = event;
    console.log('this.chosenPS', this.chosenPS);
  }

  showChosenAmount(event: number) {
    this.chosenAmount = event;
    console.log('this.chosenAmount', this.chosenAmount);
  }

}
