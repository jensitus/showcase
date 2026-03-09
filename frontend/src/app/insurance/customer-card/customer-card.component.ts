import {Component, Input} from '@angular/core';
import {Customer} from "../../customer/customer";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-customer-card',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './customer-card.component.html',
  styleUrl: './customer-card.component.scss'
})
export class CustomerCardComponent {

  @Input() customer: Customer;

}
