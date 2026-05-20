package com.lastshelter.commands;

import com.lastshelter.events.EventBus;
import com.lastshelter.events.GameEvent;
import com.lastshelter.events.GameEventType;

public class RestCommand implements BunkerCommand {
    @Override
    public String execute(EventBus eventBus) {
        String message = "Rest cycle closed.";
        eventBus.publish(new GameEvent(GameEventType.COMMAND_EXECUTED, message));
        return message;
    }
}
