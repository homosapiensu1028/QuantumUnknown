package dev.quantumunknown.progression;

public enum ProgressionStage {
    AWAKENING,
    OVERWORLD,
    DEEP_WORLD,
    NETHER,
    ENDGAME;

    public static ProgressionStage fromOrdinal(int value) {
        ProgressionStage[] values = values();
        return values[Math.max(0, Math.min(value, values.length - 1))];
    }
}
