package com.beanthere.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

@Configuration
public class ScopedBeans {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CoffeeOrder prototypeCoffeeOrder() {
        return new CoffeeOrder("prototype");
    }

    /**
     * CGLIB proxy requires a non-final, concrete class with a no-arg constructor.
     * Records are implicitly final, so we use a plain class here.
     */
    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public CoffeeOrder requestCoffeeOrder() {
        return new CoffeeOrder("request");
    }

    public static class CoffeeOrder {
        private final String scope;
        private final String instanceId;

        public CoffeeOrder() {
            this("unknown");
        }

        public CoffeeOrder(String scope) {
            this.scope = scope;
            this.instanceId = UUID.randomUUID().toString().substring(0, 8);
        }

        public String getScope()      { return scope; }
        public String getInstanceId() { return instanceId; }
    }
}
