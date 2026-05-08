package com.beanthere.store;

import com.beanthere.model.BeanInfo;
import com.beanthere.model.StartupEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central in-memory store for all bean tracking data.
 * Thread-safe — BeanPostProcessor callbacks arrive on multiple threads during parallel init.
 */
@Component
public class BeanRegistry {

    private final Map<String, BeanInfo> beans = new ConcurrentHashMap<>();
    private final List<StartupEvent> startupTimeline = new CopyOnWriteArrayList<>();

    public void register(BeanInfo info) {
        beans.putIfAbsent(info.getBeanName(), info);
    }

    public BeanInfo get(String beanName) {
        return beans.get(beanName);
    }

    public Collection<BeanInfo> getAll() {
        return Collections.unmodifiableCollection(beans.values());
    }

    public void addStartupEvent(StartupEvent event) {
        startupTimeline.add(event);
    }

    public List<StartupEvent> getStartupTimeline() {
        return Collections.unmodifiableList(startupTimeline);
    }

    public boolean contains(String beanName) {
        return beans.containsKey(beanName);
    }
}
