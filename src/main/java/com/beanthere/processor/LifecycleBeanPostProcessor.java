package com.beanthere.processor;

import com.beanthere.model.BeanInfo;
import com.beanthere.model.LifecycleEvent;
import com.beanthere.model.LifecycleStage;
import com.beanthere.store.BeanRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;

/**
 * Intercepts every bean after construction and after full initialization.
 * Runs before @PostConstruct (postProcessBeforeInitialization) and after (postProcessAfterInitialization).
 *
 * Must NOT itself depend on BeanRegistry via @Autowired — that creates a circular
 * BeanPostProcessor registration issue. Use ApplicationContextAware instead.
 */
@Component
public class LifecycleBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(LifecycleBeanPostProcessor.class);

    // Set via ApplicationContextAware to avoid circular BPP dependency
    private BeanRegistry beanRegistry;
    private ApplicationContext applicationContext;

    private static final java.util.Set<String> SKIP_PREFIXES = java.util.Set.of(
            "org.springframework", "com.sun", "java.", "jdk.",
            "springSecurityFilter", "requestMappingHandlerAdapter"
    );

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private BeanRegistry registry() {
        if (beanRegistry == null) {
            beanRegistry = applicationContext.getBean(BeanRegistry.class);
        }
        return beanRegistry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (shouldSkip(beanName, bean)) return bean;

        String scope = resolveScope(beanName);
        BeanInfo info = new BeanInfo(beanName, bean.getClass().getName(), scope);
        info.addEvent(new LifecycleEvent(LifecycleStage.INSTANTIATED, Instant.now(),
                "Instance created: " + bean.getClass().getSimpleName()));

        resolveDependencies(bean, info);

        info.addEvent(new LifecycleEvent(LifecycleStage.DEPENDENCIES_INJECTED, Instant.now(),
                "Field/constructor injection complete"));

        registry().register(info);
        log.debug("Tracked bean [{}] scope={}", beanName, scope);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (shouldSkip(beanName, bean)) return bean;

        BeanInfo info = registry().get(beanName);
        if (info != null) {
            info.addEvent(new LifecycleEvent(LifecycleStage.INITIALIZED, Instant.now(),
                    "Bean fully initialized and ready"));
        }
        return bean;
    }

    private void resolveDependencies(Object bean, BeanInfo info) {
        Class<?> clazz = bean.getClass();
        // Walk up hierarchy to catch superclass fields too
        while (clazz != null && clazz != Object.class) {
            Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> isSpringBean(f.getType()))
                    .forEach(f -> info.addDependency(f.getType().getSimpleName()));
            clazz = clazz.getSuperclass();
        }
    }

    private boolean isSpringBean(Class<?> type) {
        String pkg = type.getPackageName();
        return pkg.startsWith("com.beanthere") || pkg.startsWith("com.example");
    }

    private String resolveScope(String beanName) {
        try {
            if (applicationContext instanceof org.springframework.context.ConfigurableApplicationContext ctx) {
                return ctx.getBeanFactory().isPrototype(beanName) ? "prototype"
                        : ctx.getBeanFactory().isSingleton(beanName) ? "singleton"
                        : "other";
            }
        } catch (Exception ignored) {}
        return "singleton";
    }

    private boolean shouldSkip(String beanName, Object bean) {
        String pkg = bean.getClass().getPackageName();
        return SKIP_PREFIXES.stream().anyMatch(p -> beanName.startsWith(p) || pkg.startsWith(p))
                || beanName.equals("beanRegistry")
                || beanName.equals("lifecycleBeanPostProcessor");
    }
}
