package com.weapons;

/**
 * Armor piercing round.
 * Higher damage, lower explosion.
 */

public class APRound extends AmmoType {
	
	private static String NAME = "AP ROUND";
	private static int RADIUS = 4;
	private static int EXPLOSION_RADIUS = 20;
	private static int DAMAGE = 80;
	private static int COST = 200;
	private static String DESCRIPTION = "Armor Piercing around. Higher damage, lower explosion.";
    
    // No-argument constructor passes the specific ammo values to the parent constructor
    public APRound() {
    	super(NAME, RADIUS, EXPLOSION_RADIUS, DAMAGE, COST, DESCRIPTION);
    }
}