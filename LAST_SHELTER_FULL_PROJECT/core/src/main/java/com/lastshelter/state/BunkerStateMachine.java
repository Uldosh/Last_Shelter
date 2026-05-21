package com.lastshelter.state;

import com.lastshelter.events.EventBus;
import com.lastshelter.events.GameEvent;
import com.lastshelter.events.GameEventType;

public class BunkerStateMachine {
    private final EventBus eventBus;
    private BunkerState currentState = new NormalState();

    public BunkerStateMachine(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public BunkerState getCurrentState() {
        return currentState;
    }

    public void update(int powerLevel, int stress, boolean isolation, boolean shutdown) {
        BunkerState nextState = decideState(powerLevel, stress, isolation, shutdown);
        if (!nextState.name().equals(currentState.name())) {
            currentState = nextState;
            eventBus.publish(new GameEvent(GameEventType.STATE_CHANGED, "Bunker state: " + currentState.name()));
        }
    }

    private BunkerState decideState(int powerLevel, int stress, boolean isolation, boolean shutdown) {
        if (shutdown) {
            return new ShutdownState();
        }
        if (isolation) {
            return new IsolationState();
        }
        if (powerLevel <= 20 || stress >= 95) {
            return new CriticalState();
        }
        if (powerLevel <= 45 || stress >= 80) {
            return new EmergencyState();
        }
        return new NormalState();
    }
}
