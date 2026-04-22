package com.lastshelter.commands;

import com.lastshelter.events.EventBus;
import com.lastshelter.events.GameEvent;
import com.lastshelter.events.GameEventType;

public class ExploreCommand implements BunkerCommand {
    private final String sector;

    public ExploreCommand(String sector) {
        this.sector = sector;
    }

    @Override
    public String execute(EventBus eventBus) {
        String message = "Exploration logged: " + sector;
        eventBus.publish(new GameEvent(GameEventType.COMMAND_EXECUTED, message));
        return message;
    }
}
