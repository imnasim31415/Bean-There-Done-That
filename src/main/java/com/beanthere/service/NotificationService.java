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

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final OrderService orderService;
    private final BeanRegistry beanRegistry;

    public NotificationService(OrderService orderService, BeanRegistry beanRegistry) {
        this.orderService = orderService;
        this.beanRegistry = beanRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("NotificationService @PostConstruct — email templates loaded");
        BeanInfo info = beanRegistry.get("notificationService");
        if (info != null) {
            info.addEvent(new LifecycleEvent(LifecycleStage.POST_CONSTRUCT, Instant.now(),
                    "@PostConstruct: email templates loaded"));
        }
    }

    public String notify(String customer, String blend) {
        String result = orderService.placeOrder(blend);
        return "Notifying [" + customer + "]: " + result;
    }
}
