package com.denis.mechanisms.module;

public interface MechanismModule {
    String name();

    void enable();

    void disable();

    default void reload() {
        disable();
        enable();
    }
}
