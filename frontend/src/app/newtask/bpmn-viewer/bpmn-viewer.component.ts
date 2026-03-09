import {
    Component,
    inject,
    Input,
    OnInit,
    OnDestroy,
    ViewChild,
    ElementRef,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import BpmnJS from 'bpmn-js/lib/NavigatedViewer';
import {forkJoin, Subscription} from "rxjs";
import {CommonModule} from "@angular/common";

interface DiagramResponse {
    text: string;
}

interface FlowNodeInstance {
    flowNodeId: string;
    state: string;
    incident: boolean;
}

@Component({
  selector: 'app-bpmn-viewer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bpmn-viewer.component.html',
  styleUrl: './bpmn-viewer.component.scss'
})
export class BpmnViewerComponent implements OnInit, OnDestroy, OnChanges {

    @Input() workflowKey: string = '';
    @Input() tenantId: string = '';
    @Input() processInstanceKey?: string;
    @Input() processInstanceKeys?: string[];
    @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLDivElement>;

    private viewer: any;
    private subscription$: Subscription[] = [];
    private http = inject(HttpClient);

    loading = false;
    error: string | null = null;

    ngOnInit(): void {
        if (this.workflowKey) {
            this.loadDiagram();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if ((changes['processInstanceKey'] && !changes['processInstanceKey'].firstChange) ||
            (changes['processInstanceKeys'] && !changes['processInstanceKeys'].firstChange)) {
            this.highlightActiveElements();
        }
    }

    ngOnDestroy(): void {
        this.subscription$.forEach((s) => s.unsubscribe());
        if (this.viewer) {
            this.viewer.destroy();
        }
    }

    private loadDiagram(): void {
        this.loading = true;
        this.error = null;

        const url = `http://localhost:8080/workflows/diagram/${this.workflowKey}/tenant-id/${this.tenantId}`;

        const sub = this.http.get<DiagramResponse>(url).subscribe({
            next: (response: DiagramResponse) => {
                console.log('BPMN diagram loaded successfully');
                this.renderDiagram(response.text);
                this.loading = false;
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

                    // Highlight active elements after diagram is loaded
                    this.highlightActiveElements();
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

    private highlightActiveElements(): void {
        if (!this.viewer) {
            return;
        }

        const canvas = this.viewer.get('canvas');
        const overlays = this.viewer.get('overlays');

        // Clear existing overlays
        overlays.clear();

        // Remove existing markers
        const elementRegistry = this.viewer.get('elementRegistry');
        elementRegistry.forEach((element: any) => {
            canvas.removeMarker(element.id, 'highlight-active');
            canvas.removeMarker(element.id, 'highlight-completed');
            canvas.removeMarker(element.id, 'highlight-incident');
        });
            this.loadAndHighlightFlowNodes();
    }

    private loadAndHighlightFlowNodes(): void {
        const keys = this.processInstanceKeys?.length
            ? this.processInstanceKeys
            : this.processInstanceKey ? [this.processInstanceKey] : [];

        if (keys.length === 0) {
            return;
        }

        const requests = keys.map(key =>
            this.http.get<FlowNodeInstance[]>(`http://localhost:8080/workflows/diagram/flow-node-instance/${key}`)
        );

        const sub = forkJoin(requests).subscribe({
            next: (results) => {
                this.applyHighlights(results.flat());
            },
            error: (err) => {
                console.error('Error loading flow node instances:', err);
                this.error = 'Could not load process state information';
            }
        });

        this.subscription$.push(sub);
    }

    private applyHighlights(flowNodeInstances: FlowNodeInstance[]): void {
        if (!this.viewer) {
            return;
        }

        const canvas = this.viewer.get('canvas');
        const overlays = this.viewer.get('overlays');

        // Group flow nodes by ID to count multiple executions
        const flowNodeCounts = new Map<string, { active: number; completed: number; incident: number }>();

        flowNodeInstances.forEach(flowNode => {
            if (!flowNodeCounts.has(flowNode.flowNodeId)) {
                flowNodeCounts.set(flowNode.flowNodeId, { active: 0, completed: 0, incident: 0 });
            }
            const counts = flowNodeCounts.get(flowNode.flowNodeId)!;

            if (flowNode.state === 'ACTIVE') {
                if (flowNode.incident) {
                    counts.incident++;
                } else {
                    counts.active++;
                }
            } else if (flowNode.state === 'COMPLETED') {
                counts.completed++;
            }
        });

        // Apply highlights based on counts
        flowNodeCounts.forEach((counts, flowNodeId) => {
            try {
                // Priority: incident > active > completed
                if (counts.incident > 0) {
                    canvas.addMarker(flowNodeId, 'highlight-incident');
                    overlays.add(flowNodeId, 'note', {
                        position: {
                            top: -10,
                            right: 15
                        },
                        html: `<span class="badge badge-incident">⚠️ ${counts.incident}</span>`
                    });
                } else if (counts.active > 0) {
                    canvas.addMarker(flowNodeId, 'highlight-active');
                    overlays.add(flowNodeId, 'note', {
                        position: {
                            bottom: 10,
                            right: 15
                        },
                        html: `<span class="badge badge-active">▶ ${counts.active}</span>`
                    });
                } else if (counts.completed > 0) {
                    canvas.addMarker(flowNodeId, 'highlight-completed');
                    overlays.add(flowNodeId, 'note', {
                        position: {
                            top: -10,
                            right: 15
                        },
                        html: `<span class="badge badge-completed">✓ ${counts.completed}</span>`
                    });
                }
            } catch (err) {
                console.warn('Could not highlight flow node:', flowNodeId, err);
            }
        });

        console.log('Applied highlights to', flowNodeCounts.size, 'flow nodes');
    }

    private highlightTaskDefinition(taskDefinitionKey: string): void {
        const canvas = this.viewer.get('canvas');
        const overlays = this.viewer.get('overlays');

        try {
            canvas.addMarker(taskDefinitionKey, 'highlight-active');
            overlays.add(taskDefinitionKey, 'note', {
                position: {
                    bottom: 10,
                    right: 15
                },
                html: '<span class="badge badge-active">Current Task</span>'
            });
        } catch (err) {
            console.warn('Could not highlight task definition:', taskDefinitionKey, err);
        }
    }

}
