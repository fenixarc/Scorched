package scorched.weapons;

/**
 * High explosive round.
 * Average damage and explosion.
 */

public class HERound extends AmmoType {
	
	private static String NAME = "HE ROUND";
	private static int RADIUS = 4;
	private static int EXPLOSION_RADIUS = 40;
	private static int DAMAGE = 60;
	private static int COST = 100;
	private static String DESCRIPTION = "High Explosive around. Average damage and explosion.";
    
    // No-argument constructor passes the specific ammo values to the parent constructor
    public HERound() {
        super(NAME, RADIUS, EXPLOSION_RADIUS, DAMAGE, COST, DESCRIPTION);
    }
}