package com.beanthere.model;

import java.util.ArrayList;
import java.util.List;

public class BeanInfo {

    private final String beanName;
    private final String className;
    private final String scope;
    private final List<String> dependencies = new ArrayList<>();
    private final List<LifecycleEvent> events = new ArrayList<>();

    public BeanInfo(String beanName, String className, String scope) {
        this.beanName = beanName;
        this.className = className;
        this.scope = scope;
    }

    public synchronized void addEvent(LifecycleEvent event) {
        events.add(event);
    }

    public synchronized void addDependency(String dep) {
        if (!dependencies.contains(dep)) {
            dependencies.add(dep);
        }
    }

    public String getBeanName()          { return beanName; }
    public String getClassName()         { return className; }
    public String getScope()             { return scope; }
    public List<String> getDependencies(){ return List.copyOf(dependencies); }
    public List<LifecycleEvent> getEvents() { return List.copyOf(events); }
}
