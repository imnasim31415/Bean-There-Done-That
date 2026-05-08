package com.beanthere.listener;

import com.beanthere.model.StartupEvent;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registered directly on SpringApplication (not as a Spring bean) so it can
 * capture events that fire before the ApplicationContext is created.
 * Events are stored statically and drained into BeanRegistry later.
 */
public class EarlyStartupCapture implements SmartApplicationListener {

    static final List<StartupEvent> EARLY_EVENTS = new CopyOnWriteArrayList<>();

    @Override
    public boolean supportsEventType(Class<? extends org.springframework.context.ApplicationEvent> eventType) {
        return ApplicationStartingEvent.class.isAssignableFrom(eventType)
                || ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType)
                || ApplicationContextInitializedEvent.class.isAssignableFrom(eventType)
                || ApplicationPreparedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(org.springframework.context.ApplicationEvent event) {
        if (event instanceof ApplicationStartingEvent) {
            EARLY_EVENTS.add(new StartupEvent("APPLICATION_STARTING", Instant.now(), "SpringApplication.run() called"));
        } else if (event instanceof ApplicationEnvironmentPreparedEvent) {
            EARLY_EVENTS.add(new StartupEvent("ENVIRONMENT_PREPARED", Instant.now(), "Environment loaded, profiles resolved"));
        } else if (event instanceof ApplicationContextInitializedEvent) {
            EARLY_EVENTS.add(new StartupEvent("CONTEXT_INITIALIZED", Instant.now(), "ApplicationContext created, BeanFactory ready"));
        } else if (event instanceof ApplicationPreparedEvent) {
            EARLY_EVENTS.add(new StartupEvent("CONTEXT_LOADED", Instant.now(), "All bean definitions loaded, context not yet refreshed"));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
