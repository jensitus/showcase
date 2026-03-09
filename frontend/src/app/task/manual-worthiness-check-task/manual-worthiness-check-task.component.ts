import {Component, inject, Input, OnDestroy} from '@angular/core';
import {FormsModule} from "@angular/forms";
import {TaskService} from "../service/task.service";
import {TaskDto} from "../task-dto";
import {Subscription} from "rxjs";
import {Router} from "@angular/router";

@Component({
  selector: 'app-manual-worthiness-check-task',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './manual-worthiness-check-task.component.html',
  styleUrl: './manual-worthiness-check-task.component.scss'
})
export class ManualWorthinessCheckTaskComponent implements OnDestroy {

  @Input() additionalInfo: any;
  @Input() task: TaskDto;
  private subscription$: Subscription[] = [];
  isChecked: boolean = false;
  creditCheckOutcome: string;
  private taskService = inject(TaskService);
  private router = inject(Router);

  ngOnDestroy(): void {
    this.subscription$.forEach((s) => {
      s.unsubscribe();
    })
  }

  checkBoxValue() {
    if (this.isChecked) {
      this.creditCheckOutcome = 'APPROVED';
    } else {
      this.creditCheckOutcome = 'REJECTED';
    }
  }

  complete() {
    // this.checkBoxValue();
    // let completeTask: CompleteTaskEvent = {
    //   taskId: this.task.taskId,
    //   aggregateId: this.task.aggregateId,
    //   manualCreditCheckOutcome: this.creditCheckOutcome,
    //   manualRiskAssessmentOutcome: null,
    // }
    // this.subscription$.push(
    //   this.taskService.complete(completeTask, this.task.url, this.task.completeEndpoint).subscribe({
    //     next: value => {
    //       this.router.navigate(['/task-list']).then();
    //     },
    //     error: err => {}
    //   })
    // )
  }


  showChecked(event) {
   console.log(this.isChecked);
  }
}
