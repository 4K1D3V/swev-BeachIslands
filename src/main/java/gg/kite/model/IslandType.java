package gg.kite.model;

public enum IslandType {
    BEACH("Beach", "swev-beach.schem"),
    CHERRY("Cherry", "swev-cherry.schem"),
    TIAGA("Tiaga", "swev-tiaga.schem"),
    ISLANDS("Islands", "swev-islands.schem");

    private final String displayName;
    private final String schematicFile;

    IslandType(String displayName, String schematicFile) {
        this.displayName = displayName;
        this.schematicFile = schematicFile;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSchematicFile() {
        return schematicFile;
    }
}