import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import {Customer} from "./customer";
import {Observable} from "rxjs";
import {RequestInsurance} from "./request-insurance";
import {Message} from "./message";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class CustomerService {

  constructor(private http: HttpClient) { }

  createCustomer(customer: Customer): Observable<Customer> {
    return this.http.post<Customer>(environment.api_url + '/customers', customer);
  }

  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(environment.api_url + '/customers');
  }

  getCustomer(id: string): Observable<Customer> {
    return this.http.get<Customer>(environment.api_url + '/customers/' + id);
  }

  requestNewInsurance(requestInsurance: RequestInsurance): Observable<Message> {
    return this.http.post<Message>(environment.api_url + '/workflows', requestInsurance);
  }
}
