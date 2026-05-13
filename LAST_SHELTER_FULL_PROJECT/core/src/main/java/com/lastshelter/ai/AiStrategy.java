package com.lastshelter.ai;

public interface AiStrategy {
    String objectiveSuggestion(String objective);

    String actionComment(String taskName);

    String warning(String warning);
}
