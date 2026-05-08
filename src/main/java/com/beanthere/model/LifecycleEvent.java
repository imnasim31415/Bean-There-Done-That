package com.beanthere.model;

import java.time.Instant;

public record LifecycleEvent(
        LifecycleStage stage,
        Instant timestamp,
        String detail
) {}
