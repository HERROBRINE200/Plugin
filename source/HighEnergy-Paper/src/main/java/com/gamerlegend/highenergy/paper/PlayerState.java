package com.gamerlegend.highenergy.paper;

final class PlayerState {
    int energy;
    int maximum;
    Power selected;
    String lastKnownName;

    PlayerState(int energy, int maximum, Power selected, String lastKnownName) {
        this.energy = energy;
        this.maximum = maximum;
        this.selected = selected;
        this.lastKnownName = lastKnownName;
    }
}
