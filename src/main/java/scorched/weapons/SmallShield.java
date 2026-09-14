package scorched.weapons;

import java.awt.Color;

/**
 * High explosive round.
 * Average damage and explosion.
 */

public class SmallShield extends AmmoType {
	
	private static String NAME = "SMALL SHIELD";
	private static int RADIUS = 0;
	private static int EXPLOSION_RADIUS = 0;
	private static int DAMAGE = 0;
	private static int COST = 500;
	private static String DESCRIPTION = "Small personal shield. Protects tank from 50 damage.";
	private static Color PROJECTILE_COLOR = Color.WHITE;
	public static final int EFFECT_RADIUS = 25;
    public static final int EFFECT_TURNS = -1;
    public static final int EFFECT_DAMAGE = 0;
    public static final int ROUNDS_FIRED = 1;
    
    // No-argument constructor passes the specific ammo values to the parent constructor
    public SmallShield() {
        super(NAME, RADIUS, EXPLOSION_RADIUS, DAMAGE, COST, DESCRIPTION, PROJECTILE_COLOR, EFFECT_RADIUS, EFFECT_TURNS, EFFECT_DAMAGE, ROUNDS_FIRED);
    }
}