import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from "rxjs";
import {WorkflowService} from "../../workflow/service/workflow.service";
import {Insurance} from "../insurance";

@Component({
  selector: 'app-requested-contracts',
  standalone: true,
  imports: [

  ],
  templateUrl: './requested-contracts.component.html',
  styleUrl: './requested-contracts.component.scss'
})
export class RequestedContractsComponent implements OnInit, OnDestroy {

  @Input() aggregateId: string;
  private workflowService = inject(WorkflowService);
  private subscription$: Subscription[] = [];
  protected requestedContract: Insurance;

  ngOnDestroy(): void {
    this.subscription$.forEach((s) => {
      s.unsubscribe();
    })
  }

  ngOnInit(): void {
    this.subscription$.push(
      this.workflowService.getRequestedContract(this.aggregateId).subscribe({
        next: value => {
          this.requestedContract = value;
        }
      })
    )
  }

}
