package scorched.enums;

public enum PauseMenuOptions {
	SETTINGS("SETTINGS"),
    EXIT_BATTLE("EXIT BATTLE");

    private final String label;

    PauseMenuOptions(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
    public PauseMenuOptions next() { return values()[(ordinal() + 1) % values().length]; }
    public PauseMenuOptions previous() { return values()[(ordinal() - 1 + values().length) % values().length]; }
}
