import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router, RouterLink} from '@angular/router';
import {LoginService} from '../../auth/login.service';
import {CustomerService} from '../../customer/customer.service';
import {Customer} from '../../customer/customer';
import {Insurance} from '../insurance';
import {DatePipe} from '@angular/common';
import {WorkflowService} from '../../workflow/service/workflow.service';
import {Workflow} from '../../workflow/model/workflow';
import {BpmnViewerComponent} from '../../newtask/bpmn-viewer/bpmn-viewer.component';
import {NewTaskService} from '../../newtask/new-task.service';
import {TaskDto} from '../../newtask/task.model';
import {catchError, forkJoin, Observable, of} from 'rxjs';
import {SseService} from '../../course/sse/sse.service';
import {TranslateModule} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';

interface InsuranceType {
    value: string;
    labelKey: string;
    descriptionKey: string;
}

@Component({
    selector: 'app-insurance-showcase',
    imports: [RouterLink, DatePipe, BpmnViewerComponent, TranslateModule],
    templateUrl: './insurance-showcase.component.html',
    styleUrl: './insurance-showcase.component.scss'
})
export class InsuranceShowcaseComponent implements OnInit {
    private readonly loginService = inject(LoginService);
    private readonly customerService = inject(CustomerService);
    private readonly workflowService = inject(WorkflowService);
    private readonly taskService = inject(NewTaskService);
    private readonly sseService = inject(SseService);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);

    readonly loading = signal(false);
    readonly customer = signal<Customer | null>(null);
    readonly activeProcessInstances = signal<Workflow[]>([]);
    readonly insuranceTaskMap = signal<Map<string, TaskDto>>(new Map());
    readonly completedInsuranceProcessMap = signal<Map<string, string>>(new Map());
    readonly selectedProcessId = signal<string | null>(null);

    readonly hasCustomerProfile = computed(() => this.customer() !== null);
    readonly customerInsurances = computed(() => this.customer()?.insurances ?? []);
    readonly activeProcessInstanceIds = computed(() => this.activeProcessInstances().map(w => w.id));
    readonly allProcessInstanceIds = computed(() => {
        const completedIds = Array.from(this.completedInsuranceProcessMap().values());
        return [...this.activeProcessInstanceIds(), ...completedIds];
    });
    readonly displayedProcessInstanceIds = computed(() => {
        const sel = this.selectedProcessId();
        return sel ? [sel] : this.allProcessInstanceIds();
    });
    readonly highlightedInsuranceId = computed(() => {
        const sel = this.selectedProcessId();
        if (!sel) return null;
        for (const [insuranceId, task] of this.insuranceTaskMap()) {
            if (task.processInstanceId === sel) return insuranceId;
        }
        for (const [insuranceId, processId] of this.completedInsuranceProcessMap()) {
            if (processId === sel) return insuranceId;
        }
        return null;
    });

    readonly insuranceTypes: InsuranceType[] = [
        {value: 'HOUSEHOLD_INSURANCE', labelKey: 'INSURANCE.HOUSEHOLD_LABEL', descriptionKey: 'INSURANCE.HOUSEHOLD_DESC'},
        {value: 'LIABILITY_INSURANCE',  labelKey: 'INSURANCE.LIABILITY_LABEL',  descriptionKey: 'INSURANCE.LIABILITY_DESC'}
    ];

    ngOnInit(): void {
        const loggedInUser = this.loginService.getLoggedInUserName();
        if (loggedInUser) {
            this.loadCustomerData(loggedInUser.email);
        } else {
            this.router.navigate(['/login']);
        }
    }

    private loadCustomerData(email: string): void {
        this.loading.set(true);
        this.customerService.getCustomers()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (customers) => {
                    const userCustomer = customers.find(c => c.email === email);
                    if (userCustomer) {
                        this.customer.set(userCustomer);
                        if (userCustomer.id) {
                            this.loadActiveProcesses(userCustomer.id);
                        }
                        if (userCustomer.insurances?.length) {
                            this.loadInsuranceTasks(userCustomer.insurances);
                            this.loadCompletedProcessIds(userCustomer.insurances);
                        }
                        this.subscribeToInsuranceEvents();
                    }
                    this.loading.set(false);
                },
                error: () => this.loading.set(false)
            });
    }

    private subscribeToInsuranceEvents(): void {
        this.sseService.createInsuranceEventSource(`${environment.api_url}/server-send-insurance`)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({next: (updated) => this.handleInsuranceUpdate(updated)});
    }

    private handleInsuranceUpdate(updatedInsurance: Insurance): void {
        const customer = this.customer();
        if (!customer?.insurances) return;

        const idx = customer.insurances.findIndex(i => i.id === updatedInsurance.id);
        if (idx === -1) return;

        const updatedInsurances = [...customer.insurances];
        updatedInsurances[idx] = {...updatedInsurances[idx], ...updatedInsurance};
        this.customer.set({...customer, insurances: updatedInsurances});

        this.taskService.getTasksByInsuranceId(updatedInsurance.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (tasks) => {
                    const map = new Map(this.insuranceTaskMap());
                    if (tasks.length > 0) map.set(updatedInsurance.id, tasks[0]);
                    else map.delete(updatedInsurance.id);
                    this.insuranceTaskMap.set(map);
                }
            });

        const terminalStates = ['CANCELLED', 'REJECTED', 'APPROVED', 'COMPLETED'];
        if (terminalStates.includes(updatedInsurance.state?.toUpperCase())) {
            if (customer.id) this.loadActiveProcesses(customer.id);
            if (updatedInsurance.id) {
                this.workflowService.getProcessInstanceIdByInsuranceId(updatedInsurance.id)
                    .pipe(takeUntilDestroyed(this.destroyRef), catchError(() => of(null)))
                    .subscribe(result => {
                        if (result?.processInstanceId) {
                            const map = new Map(this.completedInsuranceProcessMap());
                            map.set(updatedInsurance.id, result.processInstanceId);
                            this.completedInsuranceProcessMap.set(map);
                        }
                    });
            }
        }
    }

    private loadInsuranceTasks(insurances: Insurance[]): void {
        const requests = insurances.reduce((acc, ins) => {
            if (ins.id) acc[ins.id] = this.taskService.getTasksByInsuranceId(ins.id);
            return acc;
        }, {} as Record<string, Observable<TaskDto[]>>);

        forkJoin(requests)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (results) => {
                    const map = new Map<string, TaskDto>();
                    for (const [insuranceId, tasks] of Object.entries(results)) {
                        if (tasks.length > 0) map.set(insuranceId, tasks[0]);
                    }
                    this.insuranceTaskMap.set(map);
                },
                error: () => {}
            });
    }

    private loadCompletedProcessIds(insurances: Insurance[]): void {
        const terminalStates = ['CANCELLED', 'REJECTED', 'APPROVED', 'COMPLETED'];
        const terminalInsurances = insurances.filter(i => i.id && terminalStates.includes(i.state?.toUpperCase()));
        if (!terminalInsurances.length) return;

        const requests = terminalInsurances.reduce((acc, ins) => {
            acc[ins.id] = this.workflowService.getProcessInstanceIdByInsuranceId(ins.id)
                .pipe(catchError(() => of(null)));
            return acc;
        }, {} as Record<string, Observable<{processInstanceId: string} | null>>);

        forkJoin(requests)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(results => {
                const map = new Map<string, string>();
                for (const [insuranceId, result] of Object.entries(results)) {
                    if (result?.processInstanceId) map.set(insuranceId, result.processInstanceId);
                }
                this.completedInsuranceProcessMap.set(map);
            });
    }

    private loadActiveProcesses(customerId: string): void {
        const customerDigits = customerId.replace(/[^0-9]/g, '');
        this.workflowService.getWorkflowListByKey('insurance_showcase')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (workflows) => {
                    this.activeProcessInstances.set(workflows.filter(w => w.businessKey === customerDigits));
                },
                error: () => {}
            });
    }

    selectProcess(id: string | null): void {
        this.selectedProcessId.set(id);
    }

    requestInsurance(insuranceType: string): void {
        const customerId = this.customer()?.id;
        if (customerId) {
            this.router.navigate(['/request-insurance', customerId, insuranceType]);
        } else {
            this.router.navigate(['/complete-profile']);
        }
    }

    getInsuranceStatusClass(state: string): string {
        switch (state?.toUpperCase()) {
            case 'REQUESTED': return 'bg-info';
            case 'ACTIVE':
            case 'APPROVED':  return 'bg-success';
            case 'PENDING':   return 'bg-warning';
            case 'CANCELLED':
            case 'REJECTED':  return 'bg-danger';
            default:          return 'bg-secondary';
        }
    }
}
