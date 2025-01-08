package com.kamikazejam.datastore.base;

@SuppressWarnings({"UnusedReturnValue", "unused", "BooleanMethodIsAlwaysInverted"})
public interface Service {

    boolean start();

    boolean shutdown();

    boolean isRunning();

}
