package com.lastshelter.state;

public class ShutdownState implements BunkerState {
    @Override
    public String name() {
        return "SHUTDOWN";
    }

    @Override
    public boolean isEmergency() {
        return true;
    }
}
