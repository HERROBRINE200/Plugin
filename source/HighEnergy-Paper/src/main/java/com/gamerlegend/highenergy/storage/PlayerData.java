package com.gamerlegend.highenergy.storage;

import com.gamerlegend.highenergy.power.Power;

/** Everything that is persisted for a single player. */
public final class PlayerData {

    private int energy;
    private int maxEnergy;
    private Power selected;
    private int regenTimer;

    public PlayerData(int energy, int maxEnergy, Power selected) {
        this.maxEnergy = Math.max(1, maxEnergy);
        this.energy = Math.max(0, Math.min(this.maxEnergy, energy));
        this.selected = selected == null ? Power.DASH : selected;
        this.regenTimer = 0;
    }

    public int energy() {
        return energy;
    }

    public void energy(int value) {
        this.energy = Math.max(0, Math.min(maxEnergy, value));
    }

    public int maxEnergy() {
        return maxEnergy;
    }

    public void maxEnergy(int value) {
        this.maxEnergy = Math.max(1, value);
        if (energy > this.maxEnergy) {
            energy = this.maxEnergy;
        }
    }

    public Power selected() {
        return selected;
    }

    public void selected(Power power) {
        this.selected = power == null ? Power.DASH : power;
    }

    public int regenTimer() {
        return regenTimer;
    }

    public void regenTimer(int value) {
        this.regenTimer = value;
    }
}
