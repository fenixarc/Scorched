package scorched.weapons;

import java.awt.Color;

/**
 * Spreads acid over a wide area.
 * Does damage every turn.
 */

public class AcidRound extends AmmoType {
	
	private static final String NAME = "ACID ROUND";
	public static final int RADIUS = 4;
	public static final int EXPLOSION_RADIUS = 0;
	public static final int DAMAGE = 0;
	public static final int COST = 500;
    public static final String DESCRIPTION = "Spreads acid over a wide area. Does damage every turn.";
    public static final Color PROJECTILE_COLOR = Color.GREEN;
    public static final int EFFECT_RADIUS = 100;
    public static final int EFFECT_TURNS = 5;
    public static final int EFFECT_DAMAGE = 20;

    public AcidRound() {
        super(NAME, RADIUS, EXPLOSION_RADIUS, DAMAGE, COST, DESCRIPTION, PROJECTILE_COLOR, EFFECT_RADIUS, EFFECT_TURNS, EFFECT_DAMAGE);
    }
}
