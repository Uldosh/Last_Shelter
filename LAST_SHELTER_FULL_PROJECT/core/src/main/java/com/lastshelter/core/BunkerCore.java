package com.lastshelter.core;

import com.lastshelter.ai.AiStrategy;
import com.lastshelter.ai.StrictStrategy;
import com.lastshelter.events.EventBus;
import com.lastshelter.state.BunkerState;
import com.lastshelter.state.BunkerStateMachine;

public final class BunkerCore {
    private static final BunkerCore INSTANCE = new BunkerCore();

    private final EventBus eventBus = new EventBus();
    private final BunkerStateMachine stateMachine = new BunkerStateMachine(eventBus);
    private AiStrategy aiStrategy = new StrictStrategy();

    private BunkerCore() {
    }

    public static BunkerCore getInstance() {
        return INSTANCE;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public BunkerState getState() {
        return stateMachine.getCurrentState();
    }

    public AiStrategy getAiStrategy() {
        return aiStrategy;
    }

    public void setAiStrategy(AiStrategy aiStrategy) {
        this.aiStrategy = aiStrategy;
    }

    public void updateState(int powerLevel, int stress, boolean isolation, boolean shutdown) {
        stateMachine.update(powerLevel, stress, isolation, shutdown);
    }
}
