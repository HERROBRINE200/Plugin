package com.gamerlegend.highenergy;

/** Per player state that must survive a server restart. */
public final class PlayerEnergyData {

    public int energy;
    public int maxEnergy;
    public String selectedPower;

    // transient: not written to disk
    public transient int regenTimer;

    public PlayerEnergyData() {
        this(EnergyManager.MAX_DEFAULT, EnergyManager.MAX_DEFAULT, Power.DASH.id());
    }

    public PlayerEnergyData(int energy, int maxEnergy, String selectedPower) {
        this.maxEnergy = Math.max(1, maxEnergy);
        this.energy = Math.max(0, Math.min(this.maxEnergy, energy));
        this.selectedPower = selectedPower == null ? Power.DASH.id() : selectedPower;
    }

    public Power selected() {
        Power power = Power.byName(selectedPower);
        return power == null ? Power.DASH : power;
    }

    public void selected(Power power) {
        this.selectedPower = (power == null ? Power.DASH : power).id();
    }
}
