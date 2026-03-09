import { Component, inject, Input, OnInit, OnDestroy, ViewChild, ElementRef, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Subscription, forkJoin } from 'rxjs';
import BpmnJS from 'bpmn-js/lib/NavigatedViewer';

interface DiagramResponse {
    text: string;
}

interface FlowNodeInstance {
    flowNodeId: string;
    state: string;
    incident: boolean;
    processInstanceKey: string;
}

interface ProcessInstance {
    businessKey: string;
    caseInstanceId: string;
    definitionId: string;
    definitionKey: string;
    ended: boolean;
    id: string;
    links: object;
    suspended: boolean;
    tenantId: string;
    state: string;
}


interface FlowNodeAggregate {
    activeCount: number;
    completedCount: number;
    incidentCount: number;
    processInstances: Set<string>;
}

@Component({
  selector: 'app-bpmn-definition-viewer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bpmn-definition-viewer.component.html',
  styleUrl: './bpmn-definition-viewer.component.scss'
})
export class BpmnDefinitionViewerComponent implements OnInit, OnDestroy, OnChanges {
    @Input() processDefinitionKey: string = '';
    @Input() tenantId: string = '';
    @Input() version?: number;
    @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLDivElement>;

    private viewer: any;
    private subscription$: Subscription[] = [];
    private http = inject(HttpClient);

    loading = false;
    error: string | null = null;
    totalInstances = 0;
    activeInstances = 0;

    ngOnInit(): void {
        if (this.processDefinitionKey && this.tenantId) {
            this.loadDiagramAndInstances();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if ((changes['processDefinitionKey'] || changes['tenantId'] || changes['version'])
            && !changes['processDefinitionKey']?.firstChange) {
            this.loadDiagramAndInstances();
        }
    }

    ngOnDestroy(): void {
        this.subscription$.forEach((s) => s.unsubscribe());
        if (this.viewer) {
            this.viewer.destroy();
        }
    }

    private loadDiagramAndInstances(): void {
        this.loading = true;
        this.error = null;

        // Build URL based on whether version is specified
        let diagramUrl = `http://localhost:8080/workflows/diagram/${this.processDefinitionKey}`; // /tenant-id/${this.tenantId}
        if (this.version !== undefined) {
            diagramUrl += `/version/${this.version}`;
        }

        const sub = this.http.get<DiagramResponse>(diagramUrl).subscribe({
            next: (response: DiagramResponse) => {
                console.log('BPMN diagram loaded successfully');
                this.renderDiagram(response.text);
                this.loadProcessInstances();
            },
            error: (err) => {
                console.error('Error loading BPMN diagram:', err);
                this.error = `Failed to load diagram: ${err.message || 'Unknown error'}`;
                this.loading = false;
            }
        });

        this.subscription$.push(sub);
    }

    private renderDiagram(xmlDiagram: string): void {
        // Initialize viewer if not already done
        if (!this.viewer) {
            this.viewer = new BpmnJS({
                container: this.canvasRef.nativeElement
            });

            this.viewer.on('import.done', ({ error }: any) => {
                if (!error) {
                    const canvas = this.viewer.get('canvas');
                    canvas.zoom('fit-viewport');
                } else {
                    console.error('Error importing BPMN diagram:', error);
                    this.error = 'Failed to render diagram';
                }
            });
        }

        // Import XML
        this.viewer.importXML(xmlDiagram).catch((err: any) => {
            console.error('Error importing XML:', err);
            this.error = 'Failed to import BPMN XML';
        });
    }

    private loadProcessInstances(): void {
        let instancesUrl = `http://localhost:8080/workflows/process-instances/${this.processDefinitionKey}/tenant-id/${this.tenantId}`;
        if (this.version !== undefined) {
            instancesUrl += `/version/${this.version}`;
        }

        const sub = this.http.get<ProcessInstance[]>(instancesUrl).subscribe({
            next: (instances: ProcessInstance[]) => {
                console.log(`Found ${instances.length} process instances`);
                this.totalInstances = instances.length;
                this.activeInstances = instances.filter(i => i.state === 'ACTIVE').length;

                if (instances.length > 0) {
                    this.loadAllFlowNodeInstances(instances);
                } else {
                    this.loading = false;
                }
            },
            error: (err) => {
                console.error('Error loading process instances:', err);
                this.error = 'Could not load process instances';
                this.loading = false;
            }
        });

        this.subscription$.push(sub);
    }

    private loadAllFlowNodeInstances(instances: ProcessInstance[]): void {
        // Create requests for all process instances
        const requests = instances.map(instance => {
            const url = `http://localhost:8080/workflows/diagram/flow-node-instance/${instance.id}`;
            return this.http.get<FlowNodeInstance[]>(url);
        });

        const sub = forkJoin(requests).subscribe({
            next: (allFlowNodes: FlowNodeInstance[][]) => {
                // Flatten the array of arrays
                const flattenedFlowNodes = allFlowNodes.flat();
                console.log(`Loaded ${flattenedFlowNodes.length} flow node instances across ${instances.length} process instances`);

                this.aggregateAndHighlight(flattenedFlowNodes);
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading flow node instances:', err);
                this.error = 'Could not load flow node data';
                this.loading = false;
            }
        });

        this.subscription$.push(sub);
    }

    private aggregateAndHighlight(flowNodeInstances: FlowNodeInstance[]): void {
        if (!this.viewer) {
            return;
        }

        const canvas = this.viewer.get('canvas');
        const overlays = this.viewer.get('overlays');

        // Clear existing overlays and markers
        overlays.clear();

        const elementRegistry = this.viewer.get('elementRegistry');
        elementRegistry.forEach((element: any) => {
            canvas.removeMarker(element.id, 'highlight-active');
            canvas.removeMarker(element.id, 'highlight-completed');
            canvas.removeMarker(element.id, 'highlight-incident');
            canvas.removeMarker(element.id, 'highlight-multiple');
        });

        // Aggregate flow nodes by flowNodeId
        const aggregates = new Map<string, FlowNodeAggregate>();

        flowNodeInstances.forEach(flowNode => {
            if (!aggregates.has(flowNode.flowNodeId)) {
                aggregates.set(flowNode.flowNodeId, {
                    activeCount: 0,
                    completedCount: 0,
                    incidentCount: 0,
                    processInstances: new Set<string>()
                });
            }

            const aggregate = aggregates.get(flowNode.flowNodeId)!;
            aggregate.processInstances.add(flowNode.processInstanceKey);

            if (flowNode.state === 'ACTIVE') {
                if (flowNode.incident) {
                    aggregate.incidentCount++;
                } else {
                    aggregate.activeCount++;
                }
            } else if (flowNode.state === 'COMPLETED') {
                aggregate.completedCount++;
            }
        });

        // Apply highlights based on aggregated data
        aggregates.forEach((aggregate, flowNodeId) => {
            try {
                const totalCount = aggregate.activeCount + aggregate.completedCount + aggregate.incidentCount;
                const instanceCount = aggregate.processInstances.size;

                // Priority: incident > active > completed
                if (aggregate.incidentCount > 0) {
                    canvas.addMarker(flowNodeId, 'highlight-incident');
                    overlays.add(flowNodeId, 'note', {
                        position: {
                            top: -10,
                            right: 15
                        },
                        html: `<div class="multi-badge">
                     <span class="badge badge-incident">⚠️ ${aggregate.incidentCount}</span>
                     <span class="badge badge-instances">${instanceCount} inst.</span>
                   </div>`
                    });
                } else if (aggregate.activeCount > 0) {
                    canvas.addMarker(flowNodeId, 'highlight-active');

                    // Add special marker if multiple instances are at this node
                    if (instanceCount > 1) {
                        canvas.addMarker(flowNodeId, 'highlight-multiple');
                    }

                    overlays.add(flowNodeId, 'note', {
                        position: {
                            bottom: 10,
                            right: 15
                        },
                        html: `<div class="multi-badge">
                     <span class="badge badge-active">▶ ${aggregate.activeCount}</span>
                     <span class="badge badge-instances">${instanceCount} inst.</span>
                   </div>`
                    });
                } else if (aggregate.completedCount > 0) {
                    canvas.addMarker(flowNodeId, 'highlight-completed');
                    overlays.add(flowNodeId, 'note', {
                        position: {
                            top: -10,
                            right: 15
                        },
                        html: `<div class="multi-badge">
                     <span class="badge badge-completed">✓ ${aggregate.completedCount}</span>
                     <span class="badge badge-instances">${instanceCount} inst.</span>
                   </div>`
                    });
                }
            } catch (err) {
                console.warn('Could not highlight flow node:', flowNodeId, err);
            }
        });

        console.log(`Applied highlights to ${aggregates.size} flow nodes from ${this.totalInstances} process instances`);
    }

}
