package com.beanthere.controller;

import com.beanthere.config.ScopedBeans;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demonstrates singleton vs prototype vs request scope.
 * Hit this endpoint multiple times and compare instanceIds:
 *   - singleton:  same id across all calls (created once at startup)
 *   - prototype:  new id every ObjectProvider.getObject() call
 *   - request:    new id per HTTP request, constant within one request
 */
@RestController
@RequestMapping("/scope")
public class ScopeController {

    private final ObjectProvider<ScopedBeans.CoffeeOrder> prototypeProvider;
    private final ScopedBeans.CoffeeOrder requestOrder;
    private final String singletonId;

    public ScopeController(
            @Qualifier("prototypeCoffeeOrder") ObjectProvider<ScopedBeans.CoffeeOrder> prototypeProvider,
            @Qualifier("requestCoffeeOrder") ScopedBeans.CoffeeOrder requestOrder
    ) {
        this.prototypeProvider = prototypeProvider;
        this.requestOrder = requestOrder;
        // Singleton: resolve once at construction time to demonstrate fixed ID
        this.singletonId = java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    @GetMapping("/demo")
    public Map<String, Object> scopeDemo() {
        ScopedBeans.CoffeeOrder proto1 = prototypeProvider.getObject();
        ScopedBeans.CoffeeOrder proto2 = prototypeProvider.getObject();

        return Map.of(
                "singleton", Map.of(
                        "scope", "singleton",
                        "instanceId", singletonId,
                        "note", "Same ID every request — ScopeController is itself a singleton"
                ),
                "prototype", Map.of(
                        "scope", "prototype",
                        "call1_instanceId", proto1.getInstanceId(),
                        "call2_instanceId", proto2.getInstanceId(),
                        "sameInstance", proto1.getInstanceId().equals(proto2.getInstanceId()),
                        "note", "Different ID each getObject() — new instance every time"
                ),
                "request", Map.of(
                        "scope", "request",
                        "instanceId", requestOrder.getInstanceId(),
                        "note", "Same ID within this HTTP request, new ID on next request"
                )
        );
    }
}
