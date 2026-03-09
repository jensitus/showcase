export interface FlowNodeInstance {
  key: number;
  processInstanceKey: number;
  processDefinitionKey: number;
  startDate: Date ;
  endDate: Date ;
  flowNodeId: string;
  flowNodeName: string;
  incidentKey: number;
  type: string;
  state: string;
  incident: boolean;
  tenantId: string;
}
