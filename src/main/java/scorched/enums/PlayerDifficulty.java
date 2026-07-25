package scorched.enums;

public enum PlayerDifficulty {
    VERY_EASY("Very Easy", 1),
    EASY("Easy", 2),
    MEDIUM("Medium", 3),
    HARD("Hard", 4),
    VERY_HARD("Very Hard", 5);

    private final String label;
    private final int level;

    PlayerDifficulty(String label, int level) {
        this.label = label;
        this.level = level;
    }

    public String getLabel() {
        return label;
    }

    public int getLevel() {
        return level;
    }

    public PlayerDifficulty next() {
        int nextOrdinal = Math.min(this.ordinal() + 1, values().length - 1);
        return values()[nextOrdinal];
    }

    public PlayerDifficulty previous() {
        int prevOrdinal = Math.max(this.ordinal() - 1, 0);
        return values()[prevOrdinal];
    }
}