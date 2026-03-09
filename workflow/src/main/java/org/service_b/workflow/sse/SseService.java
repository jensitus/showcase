package org.service_b.workflow.sse;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseService implements EventService {

    private final Collection<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public SseEmitter create() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(throwable -> {
            emitters.remove(emitter);
            emitter.completeWithError(throwable);
        });
        emitters.add(emitter);
        return emitter;
    }

    @Override
    public <T> void sendEvent(final T payload, final String eventName) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(payload);
            } catch (IOException exception) {
                emitter.completeWithError(exception);
            }
        }
    }
}
