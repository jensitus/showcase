import {Component, inject, OnInit, signal} from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {WorkflowService} from '../../workflow/service/workflow.service';
import {Workflow} from '../../workflow/model/workflow';
import {BpmnViewerComponent} from '../../newtask/bpmn-viewer/bpmn-viewer.component';

@Component({
  selector: 'app-submission-list',
  standalone: true,
  imports: [RouterLink, TranslateModule, BpmnViewerComponent],
  templateUrl: './submission-list.component.html',
  styleUrl: './submission-list.component.scss'
})
export class SubmissionListComponent implements OnInit {

  private workflowService = inject(WorkflowService);
  private router = inject(Router);

  readonly workflows = signal<Workflow[]>([]);
  readonly loading = signal(true);
  readonly selectedProcessId = signal<string | null>(null);

  ngOnInit(): void {
    this.workflowService.getWorkflowListByTenant('cfp').subscribe({
      next: (list) => {
        this.workflows.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  startSubmission(): void {
    this.router.navigate(['/submit-abstract']);
  }

  selectProcess(id: string | null): void {
    this.selectedProcessId.set(id);
  }

  displayedProcessInstanceIds(): string[] {
    const sel = this.selectedProcessId();
    return sel ? [sel] : this.workflows().map(w => w.id);
  }
}
