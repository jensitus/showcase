import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {JsonPipe, KeyValue, KeyValuePipe} from "@angular/common";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Coverage} from "../coverage";
import {Amount} from "../amount";
import {ScheduleOfPayments} from "../schedule-of-payments";
import {Event} from "@angular/router";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-payment-schedule',
  standalone: true,
  imports: [
    JsonPipe,
    KeyValuePipe,
    ReactiveFormsModule,
    FormsModule,
    TranslateModule
  ],
  templateUrl: './payment-schedule.component.html',
  styleUrl: './payment-schedule.component.scss'
})
export class PaymentScheduleComponent implements OnInit {


  coverage: string = Coverage.BASIS;
  mapScheduleOfPayments: Map<string, number> = new Map();
  paymentSchedule: Map<string, number>;
  insuranceSum: string = "€ 750 Tsd";
  coverings: string[] = [Coverage.BASIS, Coverage.EXTENDED, Coverage.FULL];
  insuranceSumSteps: number = 1;
  scheduleAmount: number[];
  @Output() chosenCoverage = new EventEmitter<string>();
  @Output() chosenInsuranceSum = new EventEmitter<string>();
  @Output() chosenPaymentSchedule = new EventEmitter<Map<string, number>>();
  @Output() chosenPS = new EventEmitter<string>();
  @Output() chosenAmount = new EventEmitter<number>();

  ngOnInit(): void {
    this.setPaymentSchedule();
  }

  volumeChange(event: Event) {
    switch (this.insuranceSumSteps) {
      case 1:
        this.insuranceSum = "€ 750 Tsd";
        break;
      case 2:
        this.insuranceSum = "€ 1,5 Mio";
        break;
      case 3:
        this.insuranceSum = "€ 3,0 Mio";
        break;
      default:
        this.insuranceSum = "€ 750 Tsd";
    }
    this.chosenInsuranceSum.emit(this.insuranceSum);
    this.paymentSchedule = null;
    this.setPaymentSchedule();
  }

  setPaymentSchedule() {
    switch (this.insuranceSum) {
      case "€ 750 Tsd":
        if (this.coverage === Coverage.BASIS) {
          // this.scheduleOfPayments = ["monthly € 20.-", "semi-annually € 110.-", "annually € 220.-"];
          this.scheduleAmount = [Amount.TWENTY, Amount.HUNDRED_TEN, Amount.TWO_HUNDRED_TWENTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.TWENTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.HUNDRED_TEN);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.TWO_HUNDRED_TWENTY);
        } else if (this.coverage === Coverage.EXTENDED) {
          // this.scheduleOfPayments = ["monthly € 30.-", "semi-annually € 170.-", "annually € 330.-"];
          this.scheduleAmount = [Amount.THIRTY, Amount.HUNDRED_SEVENTY, Amount.THREE_HUNDRED_THIRTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.THIRTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.HUNDRED_SEVENTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.THREE_HUNDRED_THIRTY);
        } else if (this.coverage === Coverage.FULL) {
          // this.scheduleOfPayments = ["monthly € 35.-", "semi-annually € 210.-", "annually € 420.-"];
          this.scheduleAmount = [Amount.THIRTY_FIVE, Amount.TWO_HUNDRED_TEN, Amount.FOUR_HUNDRED_TWENTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.THIRTY_FIVE);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.TWO_HUNDRED_TEN);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.FOUR_HUNDRED_TWENTY);
        }
        break;
      case "€ 1,5 Mio":
        if (this.coverage === Coverage.BASIS) {
          // this.scheduleOfPayments = ["monthly € 40.-", "semi-annually € 220.-", "annually € 440.-"];
          this.scheduleAmount = [Amount.FORTY, Amount.TWO_HUNDRED_TWENTY, Amount.FOUR_HUNDRED_FORTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.FORTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.TWO_HUNDRED_TWENTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.FOUR_HUNDRED_FORTY);
        } else if (this.coverage === Coverage.EXTENDED) {
          // this.scheduleOfPayments = ["monthly € 45.-", "semi-annually € 240.-", "annually € 450.-"];
          this.scheduleAmount = [Amount.FORTY_FIVE, Amount.TWO_HUNDRED_FORTY, Amount.FOUR_HUNDRED_FIFTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.FORTY_FIVE);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.TWO_HUNDRED_FORTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.FOUR_HUNDRED_FIFTY);
        } else if (this.coverage === Coverage.FULL) {
          // this.scheduleOfPayments = ["monthly € 50.-", "semi-annually € 250.-", "annually € 470.-"];
          this.scheduleAmount = [Amount.FIFTY, Amount.TWO_HUNDRED_FIFTY, Amount.FOUR_HUNDRED_SEVENTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.FIFTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.TWO_HUNDRED_FIFTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.FOUR_HUNDRED_SEVENTY);
        }
        break;
      case "€ 3,0 Mio":
        if (this.coverage === Coverage.BASIS) {
          // this.scheduleOfPayments = ["monthly € 80.-", "semi-annually € 440.-", "annually € 880.-"];
          this.scheduleAmount = [Amount.EIGHTY, Amount.FOUR_HUNDRED_FORTY, Amount.EIGHT_HUNDRED_EIGHTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.EIGHTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.FOUR_HUNDRED_FORTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.EIGHT_HUNDRED_EIGHTY);
        } else if (this.coverage === Coverage.EXTENDED) {
          // this.scheduleOfPayments = ["monthly € 90.-", "semi-annually € 450.-", "annually € 900.-"];
          this.scheduleAmount = [Amount.NINETY, Amount.FOUR_HUNDRED_FIFTY, Amount.NINE_HUNDRED];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.NINETY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.FOUR_HUNDRED_FIFTY);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.NINE_HUNDRED);
        } else if (this.coverage === Coverage.FULL) {
          // this.scheduleOfPayments = ["monthly € 105.-", "semi-annually € 510.-", "annually € 1050.-"];
          this.scheduleAmount = [Amount.HUNDRED_FIVE, Amount.FIVE_HUNDRED_TEN, Amount.ONE_THOUSAND_FIFTY];
          this.mapScheduleOfPayments.set(ScheduleOfPayments.MONTHLY, Amount.HUNDRED_FIVE);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.SEMI_ANNUALLY, Amount.FIVE_HUNDRED_TEN);
          this.mapScheduleOfPayments.set(ScheduleOfPayments.YEARLY, Amount.ONE_THOUSAND_FIFTY);
        }
        break;
      default:
        // this.scheduleOfPayments = ["monthly € 20.-", "semi-annually € 110.-", "annually € 220.-"];
        this.scheduleAmount = [Amount.TWENTY, Amount.HUNDRED_TEN, Amount.TWO_HUNDRED_TWENTY];
    }
    this.chosenPaymentSchedule.emit(this.paymentSchedule);
    // this.chosenCoverage.emit(this.coverage);
  }

  choseCoverage(event: string) {
    console.log(this.coverage);
    this.chosenCoverage.emit(this.coverage);
    this.paymentSchedule = null;
    this.setPaymentSchedule();
  }

  coverageTranslationKey(cover: string): string {
    return `INSURANCE.COVERAGE_${cover.toUpperCase()}`;
  }

  scheduleTranslationKey(key: string): string {
    return `INSURANCE.SCHEDULE_${key.toUpperCase()}`;
  }

  emitPaymentSchedule(paymentSchedule: Map<string, number>) {
    let pS: string;
    let amount: number;
    for (let [key, value] of Object.entries(this.paymentSchedule)) {
      if (key === "key") {
        pS = value;
      } else if (key === "value") {
        amount = value;
      }
    }
    this.chosenPaymentSchedule.emit(this.paymentSchedule);
    this.chosenPS.emit(pS);
    this.chosenAmount.emit(amount);
    this.chosenCoverage.emit(this.coverage);
  }
}
