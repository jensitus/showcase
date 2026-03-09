import {TaskConfig} from "./task-config";

export type Variable = {
  name: string;
  value: string;
}

export interface TaskDto {
  id: string;
  taskId: string;
  name: string;
  title: string;
  moduleId: string;
  url: string;
  description: string;
  processName: string;
  createdAt: string;
  completionTime?: string | null;
  assignee?: string | null;
  variables?: Variable[];
  taskState?: "CREATED" | "COMPLETED" | "CANCELED";
  sortValues?: [string, string];
  isFirst?: boolean | null;
  formKey?: string | null;
  processDefinitionId?: string;
  taskDefinitionId?: string;
  additionalInfo: any;
  completeEndpoint: string;
  status: string;
  aggregateId: string;
  config: any;
  configData: TaskConfig;
  taskDefinition: string;
  type: string;
}
