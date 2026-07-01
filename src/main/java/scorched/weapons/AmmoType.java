package scorched.weapons;

/**
 * Supports loading different types of ammunition into tanks.
 */

public class AmmoType {
    // Encapsulated fields for ammo properties
	private String name;
    private int radius;
    private int explosionRadius;
    private int damage;
    private int cost;
    private String description;

    // Constructor to initialize properties
    public AmmoType(String name, int radius, int explosionRadius, int damage, int cost, String description) {
    	this.name = name;
        this.radius = radius;
        this.explosionRadius = explosionRadius;
        this.damage = damage;
        this.cost = cost;
        this.description = description;
    }
    
    // Getters to access the properties
    public int getRadius() {
        return radius;
    }

    public int getExplosionRadius() {
        return explosionRadius;
    }

    public int getDamage() {
        return damage;
    }

	public String getName() {
		return name;
	}

	public int getCost() {
		return cost;
	}

	public String getDescription() {
		return description;
	}
    
}