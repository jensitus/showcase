import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router, RouterLink} from '@angular/router';
import {DatePipe} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {catchError, forkJoin, Observable, of} from 'rxjs';
import {LoginService} from '../../auth/login.service';
import {WorkflowService} from '../../workflow/service/workflow.service';
import {BpmnViewerComponent} from '../../newtask/bpmn-viewer/bpmn-viewer.component';
import {NewTaskService} from '../../newtask/new-task.service';
import {SseService} from '../../course/sse/sse.service';
import {SubmissionService} from '../submission.service';
import {Submission} from '../submission';
import {TaskDto} from '../../newtask/task.model';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-submission-list',
  standalone: true,
  imports: [TranslateModule, BpmnViewerComponent, RouterLink, DatePipe],
  templateUrl: './submission-list.component.html',
  styleUrl: './submission-list.component.scss'
})
export class SubmissionListComponent implements OnInit {

  private readonly loginService  = inject(LoginService);
  private readonly submissionService = inject(SubmissionService);
  private readonly workflowService   = inject(WorkflowService);
  private readonly taskService       = inject(NewTaskService);
  private readonly sseService        = inject(SseService);
  private readonly router            = inject(Router);
  private readonly destroyRef        = inject(DestroyRef);

  readonly loading          = signal(true);
  readonly mySubmissions    = signal<Submission[]>([]);
  readonly submissionTaskMap = signal<Map<string, TaskDto>>(new Map());
  readonly completedSubmissionProcessMap = signal<Map<string, string>>(new Map());
  readonly selectedProcessId = signal<string | null>(null);

  readonly allProcessInstanceIds = computed(() => {
    const completedIds = Array.from(this.completedSubmissionProcessMap().values());
    const activeIds = Array.from(this.submissionTaskMap().values()).map(t => t.processInstanceId);
    return [...new Set([...activeIds, ...completedIds])];
  });

  readonly displayedProcessInstanceIds = computed(() => {
    const sel = this.selectedProcessId();
    return sel ? [sel] : this.allProcessInstanceIds();
  });

  readonly highlightedSubmissionId = computed(() => {
    const sel = this.selectedProcessId();
    if (!sel) return null;
    for (const [submissionId, task] of this.submissionTaskMap()) {
      if (task.processInstanceId === sel) return submissionId;
    }
    for (const [submissionId, processId] of this.completedSubmissionProcessMap()) {
      if (processId === sel) return submissionId;
    }
    return null;
  });

  ngOnInit(): void {
    const user = this.loginService.getLoggedInUserName();
    if (user) {
      this.loadMySubmissions(user.username);
    } else {
      this.loading.set(false);
    }
  }

  private loadMySubmissions(username: string): void {
    this.submissionService.getMySubmissions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (submissions) => {
          this.mySubmissions.set(submissions);
          if (submissions.length) {
            this.loadSubmissionTasks(submissions);
            this.loadCompletedProcessIds(submissions);
          }
          this.subscribeToSubmissionEvents(username);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  private subscribeToSubmissionEvents(username: string): void {
    this.sseService.createSubmissionEventSource(`${environment.api_url}/server-send-submission`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          if (updated.createdBy !== username) return;
          this.handleSubmissionUpdate(updated);
        }
      });
  }

  private handleSubmissionUpdate(updated: Submission): void {
    const current = this.mySubmissions();
    const idx = current.findIndex(s => s.id === updated.id);
    if (idx === -1) {
      this.mySubmissions.set([updated, ...current]);
    } else {
      const copy = [...current];
      copy[idx] = {...copy[idx], ...updated};
      this.mySubmissions.set(copy);
    }

    this.taskService.getTasksBySubmissionId(updated.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (tasks) => {
          const map = new Map(this.submissionTaskMap());
          if (tasks.length > 0) map.set(updated.id, tasks[0]);
          else map.delete(updated.id);
          this.submissionTaskMap.set(map);
        }
      });

    const terminalStates = ['ACCEPTED', 'REJECTED', 'WITHDRAWN'];
    if (terminalStates.includes(updated.state?.toUpperCase())) {
      this.workflowService.getProcessInstanceIdBySubmissionId(updated.id)
        .pipe(takeUntilDestroyed(this.destroyRef), catchError(() => of(null)))
        .subscribe(result => {
          if (result?.processInstanceId) {
            const map = new Map(this.completedSubmissionProcessMap());
            map.set(updated.id, result.processInstanceId);
            this.completedSubmissionProcessMap.set(map);
          }
        });
    }
  }

  private loadSubmissionTasks(submissions: Submission[]): void {
    const requests = submissions.reduce((acc, s) => {
      acc[s.id] = this.taskService.getTasksBySubmissionId(s.id);
      return acc;
    }, {} as Record<string, Observable<TaskDto[]>>);

    forkJoin(requests)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (results) => {
          const map = new Map<string, TaskDto>();
          for (const [id, tasks] of Object.entries(results)) {
            if (tasks.length > 0) map.set(id, tasks[0]);
          }
          this.submissionTaskMap.set(map);
        },
        error: () => {}
      });
  }

  private loadCompletedProcessIds(submissions: Submission[]): void {
    const terminalStates = ['ACCEPTED', 'REJECTED', 'WITHDRAWN'];
    const terminal = submissions.filter(s => terminalStates.includes(s.state?.toUpperCase()));
    if (!terminal.length) return;

    const requests = terminal.reduce((acc, s) => {
      acc[s.id] = this.workflowService.getProcessInstanceIdBySubmissionId(s.id)
        .pipe(catchError(() => of(null)));
      return acc;
    }, {} as Record<string, Observable<{processInstanceId: string} | null>>);

    forkJoin(requests)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(results => {
        const map = new Map<string, string>();
        for (const [id, result] of Object.entries(results)) {
          if (result?.processInstanceId) map.set(id, result.processInstanceId);
        }
        this.completedSubmissionProcessMap.set(map);
      });
  }

  startSubmission(): void {
    this.router.navigate(['/submit-abstract']);
  }

  selectProcess(id: string | null): void {
    this.selectedProcessId.set(id);
  }

  getStateClass(state: string): string {
    switch (state?.toUpperCase()) {
      case 'SUBMITTED':    return 'bg-info';
      case 'UNDER_REVIEW': return 'bg-warning text-dark';
      case 'ACCEPTED':     return 'bg-success';
      case 'REJECTED':     return 'bg-danger';
      case 'WITHDRAWN':    return 'bg-secondary';
      default:             return 'bg-secondary';
    }
  }
}
