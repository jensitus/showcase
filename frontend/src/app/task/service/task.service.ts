import {Injectable} from '@angular/core';
import { HttpClient } from "@angular/common/http";
import {Observable} from "rxjs";
import {TaskDto} from "../task-dto";
import {environment} from "../../../environments/environment";
import {IAddAssignee} from "../task.component";
import {CompleteTaskEvent} from "../CompleteTaskEvent";
import {TaskListEntryDto} from "../task-list-entry-dto";
import {Message} from "../../customer/message";

@Injectable({
  providedIn: 'root'
})
export class TaskService {

  cib = environment.cib_seven_api;
  api_url = environment.api_url;
  private auth: string = 'TASKLIST-SESSION=E5BBA4E426D16F2CC38238EE08F955C4';

  constructor(
    private http: HttpClient
  ) { }

  getTaskList(): Observable<TaskListEntryDto[]> {
    return this.http.get<TaskListEntryDto[]>(this.cib + '/tasks');
  }

  getTask(taskId: string): Observable<TaskDto> {
    return this.http.get<TaskDto>(this.cib + '/tasks/' + taskId);
  }

  complete(completeTask: CompleteTaskEvent, url: string, endpointUrl: string): Observable<string> {
    return this.http.post(url + endpointUrl, completeTask, {responseType: 'text'});
  }

  addAssignee(addAssignee: IAddAssignee): Observable<IAddAssignee> {
    return this.http.post<IAddAssignee>(this.cib + '/tasks/add-assignee', addAssignee);
  }

  getTasksByCustomer(customerName: string): Observable<TaskListEntryDto[]> {
    return this.http.get<TaskListEntryDto[]>(this.cib + '/tasks/byCustomer/' + customerName);
  }

  removeAssignee(taskId: string): Observable<Message> {
    return this.http.delete<Message>(this.cib + '/tasks/remove-assignee/' + taskId);
  }

}
