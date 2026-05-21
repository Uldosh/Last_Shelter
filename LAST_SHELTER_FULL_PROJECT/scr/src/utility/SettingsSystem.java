package utility;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class SettingsSystem {
    private final Preferences preferences = Gdx.app.getPreferences("last-shelter-settings");
    private float masterVolume = preferences.getFloat("masterVolume", 0.8f);
    private float effectsVolume = preferences.getFloat("effectsVolume", 0.8f);

    public float getMasterVolume() {
        return masterVolume;
    }

    public float getEffectsVolume() {
        return effectsVolume;
    }

    public void changeMasterVolume(float amount) {
        masterVolume = clamp(masterVolume + amount);
        save();
    }

    public void changeEffectsVolume(float amount) {
        effectsVolume = clamp(effectsVolume + amount);
        save();
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void save() {
        preferences.putFloat("masterVolume", masterVolume);
        preferences.putFloat("effectsVolume", effectsVolume);
        preferences.flush();
    }
}
