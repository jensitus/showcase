export interface Variable {
  key: number;
  processInstanceKey: number;
  scopeKey: number;
  name: string;
  value: string;
  truncated: boolean;
  tenantId: string;
}
