import {Component, inject} from '@angular/core';
import {RequestHouseholdComponent} from "../request-household/request-household.component";
import {ActivatedRoute} from "@angular/router";
import {RequestLiabilityComponent} from "../request-liability/request-liability.component";

@Component({
  selector: 'app-request-insurance',
  standalone: true,
  imports: [
    RequestHouseholdComponent,
    RequestLiabilityComponent
  ],
  templateUrl: './request-insurance.component.html',
  styleUrl: './request-insurance.component.scss'
})
export class RequestInsuranceComponent {

  // @Input() insuranceType: string;
  // @Input() customerId: string;
  private activatedRoute = inject(ActivatedRoute);
  readonly customerId = this.activatedRoute.snapshot.paramMap.get('customerId');
  readonly insuranceType = this.activatedRoute.snapshot.paramMap.get('insuranceType');

}
