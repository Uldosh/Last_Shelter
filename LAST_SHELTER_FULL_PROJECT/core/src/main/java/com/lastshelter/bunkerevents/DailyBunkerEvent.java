package com.lastshelter.bunkerevents;

public class DailyBunkerEvent extends AbstractBunkerEvent {
    private final String name;
    private final boolean canResolve;
    private final Runnable resolver;

    public DailyBunkerEvent(String name, boolean canResolve, Runnable resolver) {
        this.name = name;
        this.canResolve = canResolve;
        this.resolver = resolver;
    }

    @Override
    protected String name() {
        return name;
    }

    @Override
    protected boolean evaluate() {
        return canResolve;
    }

    @Override
    protected void resolve() {
        resolver.run();
    }
}
