import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {TaskDto} from "../task-dto";
import {Subscription} from "rxjs";
import {JsonPipe} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {TaskService} from "../service/task.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-manual-risk-assessment-check-task',
  standalone: true,
  imports: [
    JsonPipe,
    FormsModule
  ],
  templateUrl: './manual-risk-assessment-check-task.component.html',
  styleUrl: './manual-risk-assessment-check-task.component.scss'
})
export class ManualRiskAssessmentCheckTaskComponent implements OnInit, OnDestroy {

  @Input() additionalInfo: any;
  @Input() task: TaskDto;
  private subscription$: Subscription[] = [];
  approve: boolean = false;
  manualRiskAssessmentOutcome: string;
  private readonly taskService = inject(TaskService);
  private readonly router = inject(Router);

  checkApprovalCheckBox() {
    if (this.approve) {
      this.manualRiskAssessmentOutcome = 'APPROVED';
    } else {
      this.manualRiskAssessmentOutcome = 'REJECTED';
    }
  }

  // complete() {
  //   this.checkApprovalCheckBox();
  //   let completeTask: CompleteTaskEvent = {
  //     taskId: this.task.taskId,
  //     aggregateId: this.task.aggregateId,
  //     completeVars: null,
  //     manualCreditCheckOutcome: null,
  //     manualRiskAssessmentOutcome: this.manualRiskAssessmentOutcome
  //   }
  //   this.subscription$.push(
  //     this.taskService.complete(completeTask, this.task.url, this.task.completeEndpoint).subscribe({
  //       next: completeTask => {
  //         this.router.navigate(['/task-list']).then();
  //       },
  //       error: error => {
  //         console.log(error);
  //       }
  //     })
  //   )
  // }

  ngOnDestroy(): void {
    this.subscription$.forEach(subscription => {
      subscription.unsubscribe()
    });
  }

  ngOnInit(): void {
  }
}
