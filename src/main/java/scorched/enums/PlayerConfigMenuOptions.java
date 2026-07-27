package scorched.enums;

public enum PlayerConfigMenuOptions {
	NAME("NAME"),
    CONTROL("CONTROL"),
    DIFFICULTY("DIFFICULTY");

    private final String label;

    PlayerConfigMenuOptions(String label) { 
    	this.label = label; 
    }
    
    public String getLabel() {
        return label;
    }
    
    public PlayerConfigMenuOptions next(boolean isAI) {
        PlayerConfigMenuOptions[] vals = values();
        int nextIndex = (this.ordinal() + 1) % vals.length;
        if (!isAI && vals[nextIndex] == DIFFICULTY) {
            nextIndex = (nextIndex + 1) % vals.length;
        }
        return vals[nextIndex];
    }

    public PlayerConfigMenuOptions previous(boolean isAI) {
        PlayerConfigMenuOptions[] vals = values();
        int prevIndex = (this.ordinal() - 1 + vals.length) % vals.length;
        if (!isAI && vals[prevIndex] == DIFFICULTY) {
            prevIndex = (prevIndex - 1 + vals.length) % vals.length;
        }
        return vals[prevIndex];
    }
}
