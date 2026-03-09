import {Component, inject, OnInit} from '@angular/core';
import {of} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {Workflow} from "./model/workflow";
import {Variable} from "./model/variable";
import {NgbNavModule} from "@ng-bootstrap/ng-bootstrap";
import {RequestedContractsComponent} from "../insurance/requested-contracts/requested-contracts.component";
import {SlicePipe} from "@angular/common";
import {BpmnDefinitionViewerComponent} from "../newtask/bpmn-definition-viewer/bpmn-definition-viewer.component";

@Component({
    selector: 'app-workflow',
    standalone: true,
    templateUrl: './workflow.component.html',
    imports: [NgbNavModule, RequestedContractsComponent, SlicePipe, BpmnDefinitionViewerComponent],
    styleUrls: ['./workflow.component.scss']
})
export class WorkflowComponent implements OnInit {
    private activatedRoute = inject(ActivatedRoute);

    workflow: Workflow;
    variables: Variable[];
    workflowId: string;
    version: string;

    ngOnInit(): void {
        this.workflowId = this.activatedRoute.snapshot.paramMap.get('id');
        this.version = this.activatedRoute.snapshot.paramMap.get('version');
    }

    protected readonly of = of;
    active = 1;
}
