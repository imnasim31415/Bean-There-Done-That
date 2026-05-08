package com.beanthere.model;

import java.util.List;

public record DependencyNode(
        String id,
        String label,
        String className,
        String scope,
        List<String> dependsOn
) {}
