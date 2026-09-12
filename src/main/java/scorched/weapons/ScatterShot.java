package scorched.weapons;

import java.awt.Color;

/**
 * Fires 8 shots in a cone.
 * High close range damage.
 */

public class ScatterShot extends AmmoType {
	
	private static String NAME = "SCATTER SHOT";
	private static int RADIUS = 2;
	private static int EXPLOSION_RADIUS = 10;
	private static int DAMAGE = 10;
	private static int COST = 100;
	private static String DESCRIPTION = "Fires 8 shots in a cone. High close range damage.";
	private static Color PROJECTILE_COLOR = Color.YELLOW;
	private static int EFFECT_RADIUS = 0;
	private static int  EFFECT_TURNS = 0;
	private static int  EFFECT_DAMAGE = 0;
	private static int ROUNDS_FIRED = 8;
    
    // No-argument constructor passes the specific ammo values to the parent constructor
    public ScatterShot() {
        super(NAME, RADIUS, EXPLOSION_RADIUS, DAMAGE, COST, DESCRIPTION, PROJECTILE_COLOR, EFFECT_RADIUS, EFFECT_TURNS, EFFECT_DAMAGE, ROUNDS_FIRED);
    }
}