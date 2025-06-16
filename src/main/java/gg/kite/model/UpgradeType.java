package gg.kite.model;

public enum UpgradeType {
    BORDER("Border Expansion", 100, 10, 5000),
    CROPS_BOOSTER("Crops Booster", 5, 1, 2000),
    MINION_LIMIT("Minion Limit", 3, 1, 3000),
    ORE_BOOSTER("Ore Booster", 5, 1, 2500);

    private final String displayName;
    private final int maxLevel;
    private final int increment;
    private final double baseCost;

    UpgradeType(String displayName, int maxLevel, int increment, double baseCost) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.increment = increment;
        this.baseCost = baseCost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getIncrement() {
        return increment;
    }

    public double getCost(int currentLevel) {
        return baseCost * Math.pow(1.5, currentLevel);
    }
}