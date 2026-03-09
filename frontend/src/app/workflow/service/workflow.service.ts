import {Injectable} from '@angular/core';
import {environment} from "../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import {Observable} from "rxjs";
import {Workflow} from "../model/workflow";
import {ProcessDefinition} from "../model/process-definition";
import {Variable} from "../model/variable";
import {Message} from "../../customer/message";
import {FlowNodeInstance} from "../model/flow-node-instance";
import {Insurance} from "../../insurance/insurance";

@Injectable({
  providedIn: 'root'
})
export class WorkflowService {

  apiUrl = environment.api_url

  constructor(private http: HttpClient) {
  }

  getWorkflowListByTenant(tenantId: string): Observable<Workflow[]> {
    return this.http.get<Workflow[]>(this.apiUrl + '/workflows/' + tenantId);
  }

  getWorkflowListByKey(key: string): Observable<Workflow[]> {
    return this.http.get<Workflow[]>(this.apiUrl + '/workflows/by-definition-key/' + key);
  }

  getDefinitionsByTenant(tenantId: string): Observable<ProcessDefinition[]> {
    return this.http.get<ProcessDefinition[]>(this.apiUrl + '/workflows/definitions/' + tenantId);
  }

  getProcInstByKey(key: number): Observable<Workflow> {
    return this.http.get<Workflow>(environment.api_url + '/workflows/workflow/' + key);
  }

  getVariablesByKey(key: number): Observable<Variable[]> {
    return this.http.get<Variable[]>(environment.api_url + '/workflows/variables/' + key);
  }

  getRequestedContract(aggregateId: string): Observable<Insurance> {
    return this.http.get<Insurance>(environment.api_url + '/workflows/requested-contract/' + aggregateId);
  }

  getDiagramByKey(key: number): Observable<Message> {
    return this.http.get<Message>(environment.api_url + '/workflows/diagram/' + key);
  }

  getSequenceFlows(key: number): Observable<string[]> {
    return this.http.get<string[]>(environment.api_url + '/workflows/diagram/sequence-flows/' + key);
  }

  getFlowNodeInstance(key: number): Observable<FlowNodeInstance[]> {
    return this.http.get<FlowNodeInstance[]>(environment.api_url + '/workflows/diagram/flow-node-instance/' + key);
  }

  getProcessInstanceIdByInsuranceId(insuranceId: string): Observable<{ processInstanceId: string }> {
    return this.http.get<{ processInstanceId: string }>(`${this.apiUrl}/api/process-instances/by-insurance/${insuranceId}`);
  }

}
