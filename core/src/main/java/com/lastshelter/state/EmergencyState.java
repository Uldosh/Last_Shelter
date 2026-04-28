package com.lastshelter.state;

public class EmergencyState implements BunkerState {
    @Override
    public String name() {
        return "EMERGENCY";
    }

    @Override
    public boolean isEmergency() {
        return true;
    }
}
