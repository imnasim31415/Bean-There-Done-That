package com.beanthere.model;

import java.time.Instant;

public record StartupEvent(
        String phase,
        Instant timestamp,
        String detail
) {}
