package org.service_b.workflow.sse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SSEController {

    private final EventService eventService;

    public SSEController(final EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping(path = "/server-send-insurance", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> insuranceEvents() {
        return ResponseEntity.status(HttpStatus.OK)
                             .header("Access-Control-Allow-Credentials", String.valueOf(true))
                             .body(eventService.create());
    }

    @GetMapping(path = "/server-send-submission", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> submissionEvents() {
        return ResponseEntity.status(HttpStatus.OK)
                             .header("Access-Control-Allow-Credentials", String.valueOf(true))
                             .body(eventService.create());
    }

}
