import {Injectable, NgZone} from '@angular/core';
import {Observable} from "rxjs";
import {TaskListEntryDto} from "../../task/task-list-entry-dto";
import {Insurance} from "../../insurance/insurance";

@Injectable({
  providedIn: 'root'
})
export class SseService {

  private eventSource: EventSource;

  constructor(private zone: NgZone) { }

  createTaskListEventSource(url: string): Observable<TaskListEntryDto> {
    console.log(url);
    const eventSource = new EventSource(url);
    return new Observable((observer: {
      next: (arg0: any) => void;
    })  => {
      eventSource.onopen = op => {
        console.log(op);
      }
      eventSource.onmessage = event => {
        this.zone.run(() => {
          console.log(event);
        })
        // console.log(event);
        const taskListEntryDto: TaskListEntryDto = JSON.parse(event.data);
        observer.next(taskListEntryDto);
      }
    })
  }

  createInsuranceEventSource(url: string): Observable<Insurance> {
    console.log(url);
    const eventSource = new EventSource(url);
    return new Observable((observer: {
      next: (arg0: any) => void;
    })  => {
      eventSource.onopen = op => {
        console.log(op);
      }
      eventSource.onmessage = event => {
        this.zone.run(() => {
          console.log(event);
        })
        // console.log(event);
        const insurance: Insurance = JSON.parse(event.data);
        observer.next(insurance);
      }
    })
  }

  // getEventSource(url: string, options: EventSourceInit): EventSource {
  //   return new EventSource(url, options);
  // }

  // connectToServerSentEvents(url: string, options: EventSourceInit, eventNames: string[] = []): Observable<Event> {
  //   this.eventSource = this.getEventSource(url, options);
  //   console.log('this.eventSource');
  //   console.log(this.eventSource);
  //   return new Observable((subscriber: Subscriber<Event>) => {
  //     this.eventSource.onerror = error => {
  //       this.zone.run(() => subscriber.error(error));
  //     };
  //
  //     eventNames.forEach((event: string) => {
  //       this.eventSource.addEventListener(event, data => {
  //         this.zone.run(() => subscriber.next(data));
  //       });
  //     });
  //   });
  // }

  close(): void {
    if (!this.eventSource) {
      return;
    }

    this.eventSource.close();
    this.eventSource = null;
  }

}
