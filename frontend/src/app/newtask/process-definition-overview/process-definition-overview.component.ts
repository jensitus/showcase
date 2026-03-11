import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { BpmnDefinitionViewerComponent } from '../bpmn-definition-viewer/bpmn-definition-viewer.component';
import { ProcessDefinition } from '../model/process-definition';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-process-definition-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, BpmnDefinitionViewerComponent, TranslateModule],
  templateUrl: './process-definition-overview.component.html',
  styleUrl: './process-definition-overview.component.scss'
})
export class ProcessDefinitionOverviewComponent implements OnInit {
    private http = inject(HttpClient);

    processDefinitions = signal<ProcessDefinition[]>([]);
    selectedDefinition = signal<ProcessDefinition | null>(null);
    loading = signal(false);
    error = signal<string | null>(null);

    ngOnInit(): void {
        this.loadProcessDefinitions();
    }

    loadProcessDefinitions(): void {
        this.loading.set(true);
        this.error.set(null);

        this.http.get<ProcessDefinition[]>(`${environment.api_url}/workflows/process-definitions`).subscribe({
            next: (definitions) => {
                this.processDefinitions.set(definitions);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error loading process definitions:', err);
                this.error.set('Failed to load process definitions');
                this.loading.set(false);
            }
        });
    }

    selectDefinition(definition: ProcessDefinition): void {
        this.selectedDefinition.set(definition);
    }

}
