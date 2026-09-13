package scorched.utils;

import scorched.weapons.AmmoType;
import scorched.weapons.WeaponRegistry;

public class WeaponBalance {
	
	public static void main(String[] args) {
		for (AmmoType t:WeaponRegistry.getAllWeapons()) {
			int cost = calculateCost(t);
			System.out.println(t.getName() + " " + cost);
		}
	}
	
	public static int calculateCost(AmmoType ammo) {
	    double score = (ammo.getDamage() * ammo.getRoundsFired()* 1.0) + 
	                   (ammo.getExplosionRadius() * 0.5) + 
	                   (ammo.getEffectDamage() * ammo.getEffectTurns() * 1.0) + 
	                   (ammo.getEffectRadius() * 0.5);
	    return (int) Math.max(10, score); // Ensure a minimum cost
	}
	
//	public static int calculateCost(AmmoType ammo) {
//	    double score = (ammo.getDamage() * 1.0) + 
//	                   (ammo.getExplosionRadius() * 0.5) + 
//	                   (ammo.getEffectDamage() * ammo.getEffectTurns() * 0.8) +
//	                   (ammo.getRoundsFired() * 2.0);
//	    return (int) Math.max(10, score); // Ensure a minimum cost
//	}


}
