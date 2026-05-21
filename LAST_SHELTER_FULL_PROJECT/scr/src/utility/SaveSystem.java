package utility;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class SaveSystem {
    private final Preferences preferences = Gdx.app.getPreferences("last-shelter-save");

    public void save(GameSnapshot snapshot) {
        preferences.putInteger("day", snapshot.day);
        preferences.putInteger("health", snapshot.health);
        preferences.putInteger("stress", snapshot.stress);
        preferences.putInteger("foodSupply", snapshot.foodSupply);
        preferences.putInteger("powerLevel", snapshot.powerLevel);
        preferences.putInteger("aiTrust", snapshot.aiTrust);
        preferences.putInteger("morality", snapshot.morality);
        preferences.putInteger("daysWithoutFood", snapshot.daysWithoutFood);
        preferences.putFloat("playerX", snapshot.playerX);
        preferences.putFloat("playerZ", snapshot.playerZ);
        preferences.putFloat("yaw", snapshot.yaw);
        preferences.putFloat("pitch", snapshot.pitch);
        preferences.putString("completedTasks", snapshot.completedTasks);
        preferences.putBoolean("exists", true);
        preferences.flush();
    }

    public boolean hasSave() {
        return preferences.getBoolean("exists", false);
    }

    public GameSnapshot load() {
        if (!hasSave()) {
            return null;
        }
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.day = preferences.getInteger("day", 1);
        snapshot.health = preferences.getInteger("health", 100);
        snapshot.stress = preferences.getInteger("stress", 18);
        snapshot.foodSupply = preferences.getInteger("foodSupply", 75);
        snapshot.powerLevel = preferences.getInteger("powerLevel", 78);
        snapshot.aiTrust = preferences.getInteger("aiTrust", 45);
        snapshot.morality = preferences.getInteger("morality", 60);
        snapshot.daysWithoutFood = preferences.getInteger("daysWithoutFood", 0);
        snapshot.playerX = preferences.getFloat("playerX", 0f);
        snapshot.playerZ = preferences.getFloat("playerZ", 2.5f);
        snapshot.yaw = preferences.getFloat("yaw", 180f);
        snapshot.pitch = preferences.getFloat("pitch", 0f);
        snapshot.completedTasks = preferences.getString("completedTasks", "");
        return snapshot;
    }
}
