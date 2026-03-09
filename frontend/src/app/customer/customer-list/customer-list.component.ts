import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {CustomerService} from "../customer.service";
import {Subscription} from "rxjs";
import {Customer} from "../customer";
import {RouterLink} from "@angular/router";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [
    RouterLink,
    TranslateModule
  ],
  templateUrl: './customer-list.component.html',
  styleUrl: './customer-list.component.scss'
})
export class CustomerListComponent implements OnInit, OnDestroy {

  private customerService = inject(CustomerService);
  private subscription: Subscription[] = [];
  customers: Customer[] = [];

  ngOnDestroy(): void {
    this.subscription.forEach((s) => s.unsubscribe());
  }

  getCustomerList() {
    this.subscription.push(
      this.customerService.getCustomers().subscribe({
        next: data => {
          this.customers = data;
        },
        error: err => {}
      })
    )
  }

  ngOnInit(): void {
    this.getCustomerList();
  }
}
