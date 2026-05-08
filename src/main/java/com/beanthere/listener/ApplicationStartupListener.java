package com.beanthere.listener;

import com.beanthere.model.StartupEvent;
import com.beanthere.store.BeanRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApplicationStartupListener {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupListener.class);
    private final BeanRegistry beanRegistry;

    public ApplicationStartupListener(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Drain early events captured before context existed
        EarlyStartupCapture.EARLY_EVENTS.forEach(beanRegistry::addStartupEvent);
        EarlyStartupCapture.EARLY_EVENTS.clear();
        record("CONTEXT_REFRESHED", "All beans instantiated and wired. Application ready.");
        log.info("Spring context fully refreshed. Total beans tracked: {}", beanRegistry.getAll().size());
    }

    @EventListener
    public void onApplicationStarted(ApplicationStartedEvent event) {
        record("APPLICATION_STARTED", "SpringApplication started, runners not yet called");
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        record("APPLICATION_READY", "Ready to serve requests");
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        record("CONTEXT_CLOSING", "Context closing, @PreDestroy methods firing");
    }

    private void record(String phase, String detail) {
        beanRegistry.addStartupEvent(new StartupEvent(phase, Instant.now(), detail));
        log.debug("Startup phase: {} — {}", phase, detail);
    }
}
