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
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CoffeeRepository coffeeRepository;
    private final BeanRegistry beanRegistry;

    public OrderService(CoffeeRepository coffeeRepository, BeanRegistry beanRegistry) {
        this.coffeeRepository = coffeeRepository;
        this.beanRegistry = beanRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("OrderService @PostConstruct — order queue initialized");
        BeanInfo info = beanRegistry.get("orderService");
        if (info != null) {
            info.addEvent(new LifecycleEvent(LifecycleStage.POST_CONSTRUCT, Instant.now(),
                    "@PostConstruct: order queue initialized"));
        }
    }

    public String placeOrder(String blend) {
        boolean available = coffeeRepository.findAllBlends().contains(blend);
        return available ? "Order placed for: " + blend : "Blend not available: " + blend;
    }
}
