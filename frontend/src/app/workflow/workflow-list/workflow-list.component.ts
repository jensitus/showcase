import {Component, defineInjectable, inject, OnDestroy, OnInit} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {forkJoin, Subscription} from "rxjs";
import {WorkflowService} from "../service/workflow.service";
import {ProcessDefinition} from "../model/process-definition";
import {TranslateModule} from "@ngx-translate/core";

const translations = {
  "title.long": 'Workflows',
  "title.short": 'Workflows',
  "total": "Total:",
  "no": "No.",
  "name": "Workflow",
  "module-unknown": "Unknown module",
  "retry-loading-module-hint": "Unfortunately, the workflow cannot be shown at the moment!",
  "retry-loading-module": "Retry loading...",
  "does-not-exist": "The requested workflow does not exist!"
}

@Component({
    selector: 'app-workflow-list',
    standalone: true,
    templateUrl: './workflow-list.component.html',
    imports: [
        RouterLink,
        TranslateModule
    ],
    styleUrls: ['./workflow-list.component.scss']
})
export class WorkflowListComponent implements OnInit, OnDestroy {

  private static readonly TENANTS = ['cfp', 'screening'];

  private subscription$: Subscription[] = [];
  definitions: ProcessDefinition[];
  private router = inject(Router);
  private workflowService = inject(WorkflowService);

  /** The process definitions of every showcase tenant, call for papers first. */
  getDefinitions(): void {
    this.subscription$.push(
      forkJoin(WorkflowListComponent.TENANTS.map(t => this.workflowService.getDefinitionsByTenant(t))).subscribe({
        next: (perTenant) => this.definitions = perTenant.flat()
      })
    )
  }

  ngOnDestroy(): void {
    this.subscription$.forEach(subscription => {})
  }

  ngOnInit(): void {
    this.getDefinitions();
  }

    protected readonly defineInjectable = defineInjectable;
}
