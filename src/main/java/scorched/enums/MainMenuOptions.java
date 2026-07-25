package scorched.enums;

public enum MainMenuOptions {
	PLAYERS("PLAYERS: "),
    HILLS("HILLS: ");

    private final String label;

    MainMenuOptions(String label) { 
    	this.label = label; 
    }
    
    public String getLabel() { return label; }
    public MainMenuOptions next() { return values()[(ordinal() + 1) % values().length]; }
    public MainMenuOptions previous() { return values()[(ordinal() - 1 + values().length) % values().length]; }
}
