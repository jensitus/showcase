import {
  AfterContentInit,
  Component,
  ElementRef,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {ImportDoneEvent} from "bpmn-js/lib/BaseViewer";
import {WorkflowService} from "../service/workflow.service";
import {Subscription} from "rxjs";
import BpmnJS from 'bpmn-js/lib/Modeler'
import {Canvas} from "bpmn-js/lib/features/context-pad/ContextPadProvider";
import NavigatedViewer from "bpmn-js/lib/NavigatedViewer";

@Component({
  selector: 'app-bpmn',
  standalone: true,
  imports: [],
  templateUrl: './bpmn.component.html',
  styleUrl: './bpmn.component.scss'
})
export class BpmnComponent implements OnInit, OnDestroy, AfterContentInit, OnChanges {
  @Input() processDefinitionKey: string;
  @Input() processInstanceKey: string;
  @ViewChild('canvas', {static: true})
  private readonly canvasElementRef: ElementRef<HTMLDivElement> | undefined;
  viewer: NavigatedViewer;
  xmlDiagram: string;
  private subscription$: Subscription[] = [];
  private workflowService = inject(WorkflowService);
  private bpmnJS: BpmnJS = new BpmnJS();

  ngOnDestroy(): void {
    this.subscription$.forEach((s) => s.unsubscribe());
  }

  ngOnInit(): void {
  //   this.viewer = new NavigatedViewer({
  //     container: '#canvas',
  //   });
  //   this.viewer.on<ImportDoneEvent>('import.done', ({error}) => {
  //     if (!error) {
  //       this.viewer.get<Canvas>('canvas').zoom('fit-viewport');
  //     }
  //   });
  //   this.subscription$.push(
  //     this.workflowService.getDiagramByKey(this.processDefinitionKey).subscribe({
  //       next: data => {
  //         this.xmlDiagram = data.text;
  //         this.viewer.importXML(this.xmlDiagram).then(async () => {
  //           const overlays = this.viewer.get('overlays');
  //           const canvas = this.viewer.get('canvas');
  //           const sequenceFlows = this.workflowService.getSequenceFlows(this.processInstanceKey);
  //           const flowNodeInstancesObservable = this.workflowService.getFlowNodeInstance(this.processInstanceKey);
  //           await flowNodeInstancesObservable.forEach((flowNodeInstances) => {
  //             flowNodeInstances.forEach(f => {
  //               if (f.state === 'ACTIVE' && f.incident === false) {
  //                 // @ts-ignore
  //                 canvas.addMarker(f.flowNodeId, 'highlight-active');
  //                 // @ts-ignore
  //                 overlays.add(f.flowNodeId, 'note', {
  //                   position: {
  //                     bottom: 10,
  //                     right: 105
  //                   },
  //                   html: '<span class="badge text-bg-success"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-0-circle" viewBox="0 0 16 16">\n' +
  //                     '  <path d="M7.988 12.158c-1.851 0-2.941-1.57-2.941-3.99V7.84c0-2.408 1.101-3.996 2.965-3.996 1.857 0 2.935 1.57 2.935 3.996v.328c0 2.408-1.101 3.99-2.959 3.99M8 4.951c-1.008 0-1.629 1.09-1.629 2.895v.31c0 1.81.627 2.895 1.629 2.895s1.623-1.09 1.623-2.895v-.31c0-1.8-.621-2.895-1.623-2.895"/>\n' +
  //                     '  <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0M1 8a7 7 0 1 0 14 0A7 7 0 0 0 1 8"/>\n' +
  //                     '</svg> 1</span>'
  //                 });
  //               } else if (f.state === 'ACTIVE' && f.incident === true) {
  //                 // @ts-ignore
  //                 canvas.addMarker(f.flowNodeId, 'highlight-active');
  //                 // @ts-ignore
  //                 overlays.add(f.flowNodeId, 'note', {
  //                   position: {
  //                     top: -10,
  //                     right: 15
  //                   },
  //                   html: '<div class="diagram-note"><span class="badge text-bg-danger">1</span></div>'
  //                 });
  //               } else if (f.state === 'COMPLETED') {
  //                 // @ts-ignore
  //                 overlays.add(f.flowNodeId, 'note', {
  //                   position: {
  //                     top: -10,
  //                     right: 15
  //                   },
  //                   html: '<div class="diagram-note"><span class="badge text-bg-secondary">1</span></div>'
  //                 });
  //               }
  //             })
  //           })
  //           // TODO this doesn't work:
  //               // await sequenceFlows.forEach((flow: any) => {
  //               // flow.forEach(flow => {
  //               // @ts-ignore
  //               // canvas.addMarker(flow, 'highlight-active');
  //               // @ts-ignore
  //               // overlays.add(flow, 'note', {
  //               //   position: {
  //               //     top: 10,
  //               //     right: -101
  //               //   },
  //               //   html: '<div class="diagram-note"><span class="badge text-bg-secondary">1</span></div>'
  //               // })
  //               // })
  //               // })
  //         })
  //
  //       }
  //     })
  //   )
  }

  ngAfterContentInit(): void {

  }

  ngOnChanges(changes: SimpleChanges) {

  }

}
