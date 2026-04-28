package com.lastshelter.state;

public class NormalState implements BunkerState {
    @Override
    public String name() {
        return "NORMAL";
    }

    @Override
    public boolean isEmergency() {
        return false;
    }
}
