package com.lastshelter.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.lastshelter.LastShelterGame;

public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("LAST SHELTER");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new LastShelterGame(), config);
    }
}
