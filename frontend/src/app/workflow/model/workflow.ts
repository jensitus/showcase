export interface Workflow {
    id: string;
    definitionId: string;
    definitionKey: string;
    businessKey: string;
    caseInstanceId: string;
    ended: boolean;
    suspended: boolean;
    tenantId: string;
    links: object;
}
