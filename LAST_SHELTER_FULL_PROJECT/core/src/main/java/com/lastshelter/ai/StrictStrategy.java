package com.lastshelter.ai;

public class StrictStrategy implements AiStrategy {
    @Override
    public String objectiveSuggestion(String objective) {
        if (objective.equals("Repair generator")) {
            return "Proceed to Generator Room.";
        }
        if (objective.equals("Fix broken pipe")) {
            return "Water Sector requires service.";
        }
        if (objective.equals("Reboot AI terminal")) {
            return "AI Core terminal reboot authorized.";
        }
        if (objective.equals("Collect food supply")) {
            return "Storage Room supply retrieval scheduled.";
        }
        return "Restore communications panel.";
    }

    @Override
    public String actionComment(String taskName) {
        if (taskName.equals("Repair generator")) {
            return "Generator output stabilized.";
        }
        if (taskName.equals("Fix broken pipe")) {
            return "Water pressure correction confirmed.";
        }
        if (taskName.equals("Reboot AI terminal")) {
            return "Terminal reboot accepted.";
        }
        if (taskName.equals("Collect food supply")) {
            return "Food supply inventory updated.";
        }
        return "Communication panel restored.";
    }

    @Override
    public String warning(String warning) {
        return warning;
    }
}
