package com.beanthere.service;

import com.beanthere.model.BeanInfo;
import com.beanthere.model.LifecycleEvent;
import com.beanthere.model.LifecycleStage;
import com.beanthere.store.BeanRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class CoffeeRepository {

    private static final Logger log = LoggerFactory.getLogger(CoffeeRepository.class);
    private final BeanRegistry beanRegistry;

    public CoffeeRepository(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("CoffeeRepository @PostConstruct — connection pool warmed up");
        BeanInfo info = beanRegistry.get("coffeeRepository");
        if (info != null) {
            info.addEvent(new LifecycleEvent(LifecycleStage.POST_CONSTRUCT, Instant.now(),
                    "@PostConstruct: connection pool warmed up"));
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("CoffeeRepository @PreDestroy — connection pool closed");
        BeanInfo info = beanRegistry.get("coffeeRepository");
        if (info != null) {
            info.addEvent(new LifecycleEvent(LifecycleStage.PRE_DESTROY, Instant.now(),
                    "@PreDestroy: connection pool closed"));
        }
    }

    public List<String> findAllBlends() {
        return List.of("Espresso", "Americano", "Latte", "Cappuccino", "Cold Brew");
    }
}
