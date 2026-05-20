package com.lastshelter.commands;

import com.lastshelter.events.EventBus;
import com.lastshelter.events.GameEvent;
import com.lastshelter.events.GameEventType;

public class RepairCommand implements BunkerCommand {
    private final String target;

    public RepairCommand(String target) {
        this.target = target;
    }

    @Override
    public String execute(EventBus eventBus) {
        String message = "Repair command completed: " + target;
        eventBus.publish(new GameEvent(GameEventType.COMMAND_EXECUTED, message));
        return message;
    }
}
