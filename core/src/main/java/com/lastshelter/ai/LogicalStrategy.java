package com.lastshelter.ai;

public class LogicalStrategy implements AiStrategy {
    @Override
    public String objectiveSuggestion(String objective) {
        return "Objective queued: " + objective + ".";
    }

    @Override
    public String actionComment(String taskName) {
        return taskName + " confirmed.";
    }

    @Override
    public String warning(String warning) {
        return "Monitor warning: " + warning;
    }
}
