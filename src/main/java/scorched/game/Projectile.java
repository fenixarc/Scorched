package scorched.game;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import scorched.sound.SoundEngine;
import scorched.weapons.AmmoType;

public class Projectile {

	private double x, y; // Projectile coordinates
	private int impactX, impactY; // Projectile impact coordinates
	private double vx, vy; // Velocity vectors along X and Y axes
	private boolean active = true;

	private final double GRAVITY = 0.15; // Downward acceleration per frame
	private final int RADIUS; // Visual size of the missile
	private final int EXPLOSION_RADIUS;
	private final int DAMAGE;

	public Projectile(int startX, int startY, int angleDegrees, double power, AmmoType ammoType) {
		this.x = startX;
		this.y = startY;
		
		this.RADIUS = ammoType.getRadius();
		this.EXPLOSION_RADIUS = ammoType.getExplosionRadius();
		this.DAMAGE = ammoType.getDamage();
		
		// Convert firing angle to radians
		double radians = Math.toRadians(angleDegrees);

		// Break power down into component speeds
		// In screen coordinates, Y decreases up and increases down, so vy must be
		// negative initially to shoot up.
		this.vx = Math.cos(radians) * power;
		this.vy = -Math.sin(radians) * power;
	}

	/**
	 * Updates physics position frame-by-frame.
	 */
	public void update(Terrain terrain, List<Tank> tankList, int screenWidth, int screenHeight) {
		if (!active)
			return;

		// Apply gravity pull to vertical velocity
		vy += GRAVITY;

		// Update positions based on velocity
		x += vx;
		y += vy;

		// Collision 1: Off-screen checks (left, right, or bottom)
		if (x < 0 || x >= screenWidth || y >= screenHeight) {
			active = false;
			// Flag the impact as out of bounds
			this.impactX = -9999;
			this.impactY = -9999;
			return;
		}

		// Sky check: If it flies above the screen (y < 0), let it fly (don't
		// deactivate) until it arcs back down

		// Iterate through the player list to find direct hits on any active tank
		for (Tank t : tankList) {
			if (t.isAlive() && t.checkHit(x, y)) {
				active = false;
				this.impactX = (int) x;
				this.impactY = (int) y;

				terrain.explode(impactX, impactY, EXPLOSION_RADIUS);
				SoundEngine.playExplosionSound();
				return;
			}
		}

		// Check if projectile hits the ground
		if (y >= 0 && x >= 0 && x < screenWidth) {
			int groundY = terrain.getHeightAt((int) x);
			if (y >= groundY) {
				active = false;

				this.impactX = (int) x;
				this.impactY = (int) y;

				// Explode the terrain at this exact impact point
				terrain.explode((int) x, (int) y, EXPLOSION_RADIUS);

				// Trigger explosion audio
				SoundEngine.playExplosionSound();
			}
		}
	}

	public void draw(Graphics2D g2d) {
		if (!active)
			return;

		g2d.setColor(Color.YELLOW);
		g2d.fillOval((int) x - RADIUS, (int) y - RADIUS, RADIUS * 2, RADIUS * 2);
	}

	public boolean isActive() {
		return active;
	}

	public int getImpactX() {
		return impactX;
	}

	public int getImpactY() {
		return impactY;
	}

	public int getExplosionRadius() {
		return EXPLOSION_RADIUS;
	}

	public int getDamage() {
		return DAMAGE;
	}
}