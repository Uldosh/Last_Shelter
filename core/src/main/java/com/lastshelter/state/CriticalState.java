package com.lastshelter.state;

public class CriticalState implements BunkerState {
    @Override
    public String name() {
        return "CRITICAL";
    }

    @Override
    public boolean isEmergency() {
        return true;
    }
}
