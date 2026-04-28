package com.lastshelter.state;

public class IsolationState implements BunkerState {
    @Override
    public String name() {
        return "ISOLATION";
    }

    @Override
    public boolean isEmergency() {
        return true;
    }
}
