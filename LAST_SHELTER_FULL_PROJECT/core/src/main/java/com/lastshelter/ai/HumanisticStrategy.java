package com.lastshelter.ai;

public class HumanisticStrategy implements AiStrategy {
    @Override
    public String objectiveSuggestion(String objective) {
        return "Recommended next task: " + objective + ".";
    }

    @Override
    public String actionComment(String taskName) {
        return "Good. " + taskName + " is complete.";
    }

    @Override
    public String warning(String warning) {
        return "Please address this soon: " + warning;
    }
}
