import {Component, inject, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {BpmnDefinitionViewerComponent} from "../newtask/bpmn-definition-viewer/bpmn-definition-viewer.component";

/** One deployed process definition version: its diagram with the running instances on it. */
@Component({
    selector: 'app-workflow',
    standalone: true,
    templateUrl: './workflow.component.html',
    imports: [BpmnDefinitionViewerComponent],
    styleUrls: ['./workflow.component.scss']
})
export class WorkflowComponent implements OnInit {
    private activatedRoute = inject(ActivatedRoute);

    tenantId: string;
    workflowId: string;
    version: number;

    ngOnInit(): void {
        this.tenantId = this.activatedRoute.snapshot.paramMap.get('tenantId');
        this.workflowId = this.activatedRoute.snapshot.paramMap.get('id');
        this.version = Number(this.activatedRoute.snapshot.paramMap.get('version'));
    }
}
