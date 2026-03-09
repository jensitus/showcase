import {Component, defineInjectable, inject, OnDestroy, OnInit} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {Workflow} from "../model/workflow";
import {Subscription} from "rxjs";
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

  private subscription$: Subscription[] = [];
  workflows: Workflow[] = [];
  definitions: ProcessDefinition[];
  private router = inject(Router);
  private workflowService = inject(WorkflowService);

  getWorkflowsByDefinitionKey(key: string, name: string): void {
    this.subscription$.push(
      this.workflowService.getWorkflowListByKey(key).subscribe({
        next: (value) => {
          for (let workflow of value) {
              console.log('workflow value', value);
            // workflow.name = name;
            this.workflows.push(workflow);
          }
          console.log('workflows', this.workflows);
        },
        error: err => {
          console.log('error', err);
        }
      })
    )
  }

  getDefinitionsByTenant(): void {
    this.subscription$.push(
      this.workflowService.getDefinitionsByTenant("insurance").subscribe({
        next: (value) => {
          this.definitions = value;
          console.log('definitions: ', this.definitions);
          for (let definition of this.definitions) {
            // this.getWorkflowsByDefinitionKey(definition.key, definition.name)
          }
        }
      })
    )
  }

  ngOnDestroy(): void {
    this.subscription$.forEach(subscription => {})
  }

  ngOnInit(): void {
    this.getDefinitionsByTenant();
  }

    protected readonly defineInjectable = defineInjectable;
}
