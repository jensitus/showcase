import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {DeploymentService} from '../service/deployment.service';

@Component({
    selector: 'app-bpmn-deployment',
    standalone: true,
    imports: [FormsModule, TranslateModule],
    templateUrl: './bpmn-deployment.component.html',
})
export class BpmnDeploymentComponent {

    private deploymentService = inject(DeploymentService);

    deploymentName = signal('');
    tenantId = signal('insurance');
    selectedFile = signal<File | null>(null);
    status = signal<'idle' | 'loading' | 'success' | 'error'>('idle');
    message = signal('');

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files?.length) {
            this.selectedFile.set(input.files[0]);
            if (!this.deploymentName()) {
                this.deploymentName.set(input.files[0].name.replace('.bpmn', ''));
            }
        }
    }

    deploy(): void {
        const file = this.selectedFile();
        if (!file || !this.deploymentName()) return;

        this.status.set('loading');
        this.message.set('');

        this.deploymentService.deploy(file, this.deploymentName(), this.tenantId()).subscribe({
            next: () => {
                this.status.set('success');
                this.message.set('DEPLOY.SUCCESS');
                this.selectedFile.set(null);
            },
            error: (err) => {
                this.status.set('error');
                this.message.set(err.error || 'DEPLOY.ERROR');
            }
        });
    }
}
