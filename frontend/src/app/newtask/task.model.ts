export interface TaskDto {
    id: string;
    taskId: string;
    name: string;
    assignee: string;
    created: string;
    executionId: string;
    processDefinitionId: string;
    processInstanceId: string;
    taskDefinitionKey: string;
    formKey: string;
    tenantId: string;
    taskState: string;
    additionalInfo: any;
    config: any;
    configData: any;
    taskDefinition: string;
}

export interface Page<T> {
    content: T[];
    pageable: {
        pageNumber: number;
        pageSize: number;
        sort: {
            sorted: boolean;
            unsorted: boolean;
            empty: boolean;
        };
        offset: number;
        paged: boolean;
        unpaged: boolean;
    };
    totalElements: number;
    totalPages: number;
    last: boolean;
    size: number;
    number: number;
    sort: {
        sorted: boolean;
        unsorted: boolean;
        empty: boolean;
    };
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}
