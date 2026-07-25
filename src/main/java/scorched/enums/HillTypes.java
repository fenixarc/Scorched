package scorched.enums;

import java.util.Random;

public enum HillTypes {
	RANDOM("Random", 0),
    ROLLING("Rolling Hills", 1),
    LARGE("Large Hills", 2),
    JAGGED("Jagged Cliffs", 3);

	private final String label;
    private final int strength;
    private static final Random PRNG = new Random();

    HillTypes(String label, int strength) {
        this.label = label;
        this.strength = strength;
    }
    
    /**
     * Resolves the hill type. If this instance is RANDOM, it picks 
     * uniformly among all other HillTypes.
     */
    public HillTypes resolve() {
        if (this != RANDOM) {
            return this;
        }

        // Get all enum constants
        HillTypes[] allTypes = values();

        // Filter out RANDOM
        HillTypes[] hillTypes = java.util.Arrays.stream(allTypes)
                .filter(t -> t != RANDOM)
                .toArray(HillTypes[]::new);

        return hillTypes[PRNG.nextInt(hillTypes.length)];
    }
    
    public String getLabel() { return label; }
    public int getStrength() { return strength; }
    public HillTypes next() { return values()[(ordinal() + 1) % values().length]; }
    public HillTypes previous() { return values()[(ordinal() - 1 + values().length) % values().length]; }
}
