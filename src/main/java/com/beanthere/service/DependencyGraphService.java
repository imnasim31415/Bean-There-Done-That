package com.beanthere.service;

import com.beanthere.model.BeanInfo;
import com.beanthere.model.DependencyNode;
import com.beanthere.store.BeanRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DependencyGraphService {

    private final BeanRegistry beanRegistry;

    public DependencyGraphService(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
    }

    public List<DependencyNode> buildGraph() {
        return beanRegistry.getAll().stream()
                .map(b -> new DependencyNode(
                        b.getBeanName(),
                        simpleName(b.getClassName()),
                        b.getClassName(),
                        b.getScope(),
                        b.getDependencies()
                ))
                .toList();
    }

    public List<String> detectCircularHints() {
        // Lightweight cycle hint: find beanA → beanB → beanA in dependency names
        var all = beanRegistry.getAll();
        return all.stream()
                .flatMap(a -> a.getDependencies().stream()
                        .filter(depSimpleName -> all.stream()
                                .filter(b -> simpleName(b.getClassName()).equals(depSimpleName))
                                .anyMatch(b -> b.getDependencies().stream()
                                        .anyMatch(d -> d.equals(simpleName(a.getClassName())))))
                        .map(d -> simpleName(a.getClassName()) + " ↔ " + d))
                .distinct()
                .toList();
    }

    private String simpleName(String fqcn) {
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }
}
