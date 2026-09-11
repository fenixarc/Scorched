package scorched.weapons;

import java.awt.Color;

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
    private Color projectileColor;
    private int effectRadius;
    private int effectTurns;
    private int effectDamage;

    // Constructor to initialize properties with default color (Color.YELLOW)
    public AmmoType(String name, int radius, int explosionRadius, int damage, int cost, String description) {
        this(name, radius, explosionRadius, damage, cost, description, Color.YELLOW, 0, 0, 0);
    }

    public AmmoType(String name, int radius, int explosionRadius, int damage, int cost, String description, Color projectileColor) {
        this(name, radius, explosionRadius, damage, cost, description, projectileColor, 0, 0, 0);
    }

    public AmmoType(String name, int radius, int explosionRadius, int damage, int cost, String description, Color projectileColor, int effectRadius, int effectTurns, int effectDamage) {
    	this.name = name;
        this.radius = radius;
        this.explosionRadius = explosionRadius;
        this.damage = damage;
        this.cost = cost;
        this.description = description;
        this.projectileColor = projectileColor != null ? projectileColor : Color.YELLOW;
        this.effectRadius = effectRadius;
        this.effectTurns = effectTurns;
        this.effectDamage = effectDamage;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AmmoType other = (AmmoType) obj;
        return this.getName() != null && this.getName().equalsIgnoreCase(other.getName());
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().toLowerCase().hashCode() : 0;
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
    
    public Color getProjectileColor() {
        return projectileColor;
    }

    public int getEffectRadius() {
        return effectRadius;
    }

    public int getEffectTurns() {
        return effectTurns;
    }

    public int getEffectDamage() {
        return effectDamage;
    }

}
