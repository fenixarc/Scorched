package com.weapons;

/**
 * Small nuclear device.
 * High damage and explosion.
 */

public class MiniNuke extends AmmoType {
	
	private static String NAME = "Mini Nuke";
	private static int RADIUS = 4;
	private static int EXPLOSION_RADIUS = 100;
	private static int DAMAGE = 100;
	private static int COST = 1000;
	private static String DESCRIPTION = "Small nuclear device. High damage and explosion.";
    
    // No-argument constructor passes the specific ammo values to the parent constructor
    public MiniNuke() {
        super(NAME, RADIUS, EXPLOSION_RADIUS, DAMAGE, COST, DESCRIPTION);
    }
}