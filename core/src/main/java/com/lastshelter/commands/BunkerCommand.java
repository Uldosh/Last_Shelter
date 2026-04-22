package com.lastshelter.commands;

import com.lastshelter.events.EventBus;

public interface BunkerCommand {
    String execute(EventBus eventBus);
}
