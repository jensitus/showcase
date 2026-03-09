import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {SseService} from "./sse.service";
import {of, SubscriptionLike} from "rxjs";
import {MessageData} from "./message-data";

@Component({
  selector: 'app-sse',
  standalone: true,
  imports: [
  ],
  templateUrl: './sse.component.html',
  styleUrl: './sse.component.scss'
})
export class SseComponent implements OnInit, OnDestroy {
  private eventSourceSubscription: SubscriptionLike;

  private sseService = inject(SseService);
  messagesVerdammt: MessageData[] = [];

  ngOnInit(): void {

  }

  subscribeToEventSource() {

  }

  ngOnDestroy() {
    this.eventSourceSubscription.unsubscribe();
    this.sseService.close();
  }

  protected readonly of = of;
}
