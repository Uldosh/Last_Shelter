package com.lastshelter.world;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class Door {
    private final ModelInstance firstPanel;
    private final ModelInstance secondPanel;
    private final float x;
    private final float z;
    private final boolean slidesOnZ;
    private final float width;
    private final float depth;
    private final boolean automatic;
    private DoorState state;
    private float openAmount;

    public Door(ModelInstance firstPanel, ModelInstance secondPanel, float x, float z, boolean slidesOnZ,
        float width, float depth, boolean automatic, DoorState state) {
        this.firstPanel = firstPanel;
        this.secondPanel = secondPanel;
        this.x = x;
        this.z = z;
        this.slidesOnZ = slidesOnZ;
        this.width = width;
        this.depth = depth;
        this.automatic = automatic;
        this.state = state;
        this.openAmount = state == DoorState.OPEN ? 1f : 0f;
        updatePanels(openAmount);
    }

    public DoorState getState() {
        return state;
    }

    public boolean isLocked() {
        return state == DoorState.LOCKED;
    }

    public void lock() {
        state = DoorState.LOCKED;
    }

    public void open() {
        if (!isLocked()) {
            state = DoorState.OPEN;
        }
    }

    public void close() {
        if (!isLocked()) {
            state = DoorState.CLOSED;
        }
    }

    public void update(float playerX, float playerZ, float delta) {
        if (automatic && !isLocked()) {
            float dx = playerX - x;
            float dz = playerZ - z;
            float distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < 16f) {
                open();
            } else if (distanceSquared > 24f) {
                close();
            }
        }
        float target = state == DoorState.OPEN ? 1f : 0f;
        openAmount += (target - openAmount) * Math.min(1f, delta * 6f);
        updatePanels(openAmount);
    }

    public boolean blocks(float playerX, float playerZ, float playerRadius) {
        if (state == DoorState.OPEN) {
            return false;
        }
        return playerX >= x - width / 2f - playerRadius && playerX <= x + width / 2f + playerRadius
            && playerZ >= z - depth / 2f - playerRadius && playerZ <= z + depth / 2f + playerRadius;
    }

    private void updatePanels(float amount) {
        float shift = amount * 1.35f;
        if (slidesOnZ) {
            firstPanel.transform.setToTranslation(x, 1.4f, z - 0.72f - shift).scale(0.18f, 2.8f, 1.35f);
            secondPanel.transform.setToTranslation(x, 1.4f, z + 0.72f + shift).scale(0.18f, 2.8f, 1.35f);
        } else {
            firstPanel.transform.setToTranslation(x - 0.72f - shift, 1.4f, z).scale(1.35f, 2.8f, 0.18f);
            secondPanel.transform.setToTranslation(x + 0.72f + shift, 1.4f, z).scale(1.35f, 2.8f, 0.18f);
        }
    }
}
