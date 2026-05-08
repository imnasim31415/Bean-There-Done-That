package com.beanthere.service;

import com.beanthere.model.BeanInfo;
import com.beanthere.model.LifecycleEvent;
import com.beanthere.model.LifecycleStage;
import com.beanthere.store.BeanRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Top-level service — depends on OrderService and NotificationService.
 * Demonstrates a 3-level deep dependency chain:
 *   BaristaService → OrderService → CoffeeRepository
 *                  → NotificationService → OrderService (already singleton)
 */
@Service
public class BaristaService {

    private static final Logger log = LoggerFactory.getLogger(BaristaService.class);

    private final OrderService orderService;
    private final NotificationService notificationService;
    private final BeanRegistry beanRegistry;

    public BaristaService(OrderService orderService,
                          NotificationService notificationService,
                          BeanRegistry beanRegistry) {
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.beanRegistry = beanRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("BaristaService @PostConstruct — barista station ready");
        BeanInfo info = beanRegistry.get("baristaService");
        if (info != null) {
            info.addEvent(new LifecycleEvent(LifecycleStage.POST_CONSTRUCT, Instant.now(),
                    "@PostConstruct: barista station ready, grinder calibrated"));
        }
    }

    public String serve(String customer, String blend) {
        return notificationService.notify(customer, blend);
    }
}
