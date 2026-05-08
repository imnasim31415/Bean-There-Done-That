package com.beanthere.model;

public enum LifecycleStage {
    INSTANTIATED,
    DEPENDENCIES_INJECTED,
    POST_CONSTRUCT,
    INITIALIZED,
    PRE_DESTROY
}
