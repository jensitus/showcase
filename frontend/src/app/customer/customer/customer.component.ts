import {Component, ElementRef, inject, OnDestroy, OnInit, Renderer2, ViewChild} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {Subscription} from "rxjs";
import {CustomerService} from "../customer.service";
import {Customer} from "../customer";
import {FormsModule} from "@angular/forms";
import {DatePipe, JsonPipe, KeyValuePipe} from "@angular/common";
import {TaskService} from "../../task/service/task.service";
import {TaskListEntryDto} from "../../task/task-list-entry-dto";
import {SseService} from "../../course/sse/sse.service";
import {environment} from "../../../environments/environment";
import {Insurance} from "../../insurance/insurance";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [
    FormsModule,
    JsonPipe,
    KeyValuePipe,
    RouterLink,
    DatePipe,
    TranslateModule
  ],
  templateUrl: './customer.component.html',
  styleUrl: './customer.component.scss'
})
export class CustomerComponent implements OnInit, OnDestroy {

  // private customerId: string;
  private customerService = inject(CustomerService);
  private activatedRoute = inject(ActivatedRoute);
  private taskService = inject(TaskService);
  private sseService = inject(SseService);
  private renderer = inject(Renderer2);
  readonly router = inject(Router);
  private subscription: Subscription[] = [];
  customer: Customer;
  isChecked: boolean = false;
  type: string;
  tasks: TaskListEntryDto[] = [];
  approvedInsurances: Insurance[] = [];
  requestedInsurances: Insurance[] = [];
  @ViewChild('tableElement') tableElement: ElementRef;

  ngOnDestroy(): void {
    this.subscription.forEach(s => s.unsubscribe());
  }

  ngOnInit(): void {
    let customerId = this.activatedRoute.snapshot.paramMap.get('id');
    this.getCustomer(customerId);
    this.getNewTaskPerSse(environment.api_url + '/order-status');
    this.getNewInsurancePerSse(environment.api_url + '/server-send-insurance')
  }

  getCustomer(customerId: string) {
    this.subscription.push(
      this.customerService.getCustomer(customerId).subscribe({
        next: value => {
          this.customer = value;
          this.getTasks(this.customer.firstname + ' ' + this.customer.lastname);
          this.splitInsurances(this.customer.insurances);
        },
        error: error => console.log(error),
      })
    )
  }

  splitInsurances(insurances: Insurance[]) {
    for (let insurance of this.customer.insurances) {
      if (insurance.state === 'APPROVED') {
        this.approvedInsurances.push(insurance);
      }
      if (insurance.state === 'REQUESTED') {
        this.requestedInsurances.push(insurance);
      }
    }
  }

  getTasks(customerName: string) {
    this.subscription.push(
      this.taskService.getTasksByCustomer(customerName).subscribe({
        next: value => {
          this.tasks = value;
        }
      })
    )
  }

  new_insurance() {
    if (this.isChecked) {

    }
    console.log(this.type);
  }

  request_insurance() {
    this.router.navigate(['/request-insurance', this.customer.id, this.type]).then();
  }

  getNewTaskPerSse(url: string) {
    this.subscription.push(
      this.sseService.createTaskListEventSource(url).subscribe({
          next: data => {
            this.addNewTrElement(data);
          },
          error: error => {
            //handle error
          }
        }
      )
    );
  }

  getNewInsurancePerSse(url: string) {
    this.subscription.push(
      this.sseService.createInsuranceEventSource(url).subscribe({
        next: data => {
          this.approvedInsurances.push(data);
          console.log(this.approvedInsurances);
        }
      })
    )
  }

  addNewTrElement(data: TaskListEntryDto) {
    let background = '#ffffff';
    let tr = this.renderer.createElement('tr');
    this.renderer.setAttribute(tr, 'id', 'new_tr');
    this.renderer.setStyle(tr, 'background', background);
    this.renderer.appendChild(this.tableElement.nativeElement, tr);
    let td_hash = this.renderer.createElement('td');
    this.renderer.setStyle(td_hash, 'background', background);
    let td_title = this.renderer.createElement('td');
    this.renderer.setStyle(td_title, 'background', background);
    let td_type = this.renderer.createElement('td');
    this.renderer.setStyle(td_type, 'background', background);
    let td_assignee = this.renderer.createElement('td');
    this.renderer.setStyle(td_assignee, 'background', background);
    let td_createdAt = this.renderer.createElement('td');
    this.renderer.setStyle(td_createdAt, 'background', background);
    let a = this.renderer.createElement('a');
    let linkText = this.renderer.createText(data.taskId);
    this.renderer.setAttribute(a, 'href', 'http://localhost:4200/tasks/' + data.taskId)
    this.renderer.appendChild(a, linkText);
    this.renderer.appendChild(td_hash, a);
    this.renderer.appendChild(tr, td_hash);
    this.renderer.appendChild(td_title, this.renderer.createText(data.title));
    this.renderer.appendChild(tr, td_title);
    this.renderer.appendChild(td_type, this.renderer.createText(data.type));
    this.renderer.appendChild(tr, td_type);
    this.renderer.appendChild(td_assignee, this.renderer.createText(data.assignee));
    this.renderer.appendChild(tr, td_assignee);
    this.renderer.appendChild(td_createdAt, this.renderer.createText('right now'));
    this.renderer.appendChild(tr, td_createdAt);
  }

  addInsuranceCard() {

  }

}
