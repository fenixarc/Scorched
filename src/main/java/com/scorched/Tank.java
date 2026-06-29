package com.scorched;
import java.awt.Color;
import java.awt.Graphics2D;

public class Tank {

	private int x;
	private int y;
	private int width = 30;
	private int height = 14;
	private Color color;
	private int maxHealth = 100;
	private int currentHealth = 100;
	private boolean alive = true;
	private static int GRAVITY = 4;
	private int fallStartY = -1;
	private DamageListener damageListener;

	// Aiming properties (Angle in degrees: 0 is right, 90 is straight up, 180 is
	// left)
	private int barrelAngle;
	private int barrelLength = 20;
	private double power = 10.0;

	public Tank(int startX, Terrain terrain, Color color, int startingAngle) {
		this.x = startX;
		this.color = color;
		this.barrelAngle = startingAngle;

		// Snap the tank's bottom directly onto the ground surface
		this.y = terrain.getHeightAt(this.x) - this.height;
	}

	/**
	 * Checks the ground level beneath the tank and applies gravity if it's
	 * floating. Calculates and applies damage based on total fall distance once the
	 * tank lands.
	 */
	public boolean applyGravity(Terrain terrain) {
		//if (!alive)
			//return false;

		int targetGroundY = terrain.getHeightAt(this.x) - this.height;

		// Condition A: The tank is currently in mid-air
		if (this.y < targetGroundY) {
			// If this is the very first frame of the fall, record our starting height
			if (fallStartY == -1) {
				fallStartY = this.y;
			}

			this.y += GRAVITY; // Apply gravity drop speed

			if (this.y > targetGroundY) {
				this.y = targetGroundY; // Snap to ground
			}
			return true; // Still falling
		}

		// Condition B: The tank is securely on the ground
		// If fallStartY is NOT -1, it means we WERE falling and just landed on this
		// exact frame
		if (fallStartY != -1) {
			int totalFallDistance = this.y - fallStartY;
			fallStartY = -1; // Reset tracking flag immediately for the next fall

			int safeFallDistance = 20; // Allow a short, safe drop without penalty

			if (alive && totalFallDistance > safeFallDistance) {
				int excessiveFall = totalFallDistance - safeFallDistance;

				// Calculate damage: 1 point of damage per 2 pixels of excessive fall height
				int fallDamage = excessiveFall / 2;

				if (fallDamage > 0) {
					SoundEngine.playFallDamageSound();
					this.takeDamage(fallDamage);
					if (damageListener != null) {
			            damageListener.onTankTakeDamage(this.x, this.y, fallDamage);
			        }
					System.out.println(this.color + " Tank sustained fall damage: " + fallDamage);
				}
			}
		}

		return false; // Grounded and stationary
	}

	public void draw(Graphics2D g2d) {
		// Draw Tank Body
		g2d.setColor(color);
		g2d.fillRect(x - (width / 2), y, width, height);

		// Draw a small turret on top center
		g2d.fillOval(x - 7, y - 6, 14, 12);

		if (alive) {
			
			// Draw barrel
			double radians = Math.toRadians(barrelAngle);
	
			// Calculate where the tip of the barrel ends based on its pivot point (center
			// of turret)
			int pivotX = x;
			int pivotY = y;
	
			int tipX = (int) (pivotX + Math.cos(radians) * barrelLength);
			// Note: Subtracting because Y goes downwards in screen space
			int tipY = (int) (pivotY - Math.sin(radians) * barrelLength);
	
			g2d.setColor(Color.WHITE);
			g2d.setStroke(new java.awt.BasicStroke(3)); // Thicken the barrel line
			g2d.drawLine(pivotX, pivotY, tipX, tipY);
			g2d.setStroke(new java.awt.BasicStroke(1)); // Reset stroke width
			
			// Draw Health Bar Background (Red frame)
			g2d.setColor(Color.DARK_GRAY);
			g2d.fillRect(x - 20, y - 12, 40, 5);

			// Draw Current Health (Green fill line)
			g2d.setColor(Color.GREEN);
			int healthBarWidth = (int) (40 * ((double) currentHealth / maxHealth));
			g2d.fillRect(x - 20, y - 12, healthBarWidth, 5);
			
		} else { 
			// If the tank is dead, draw a burning wreckage marker instead
			g2d.setColor(Color.DARK_GRAY);
			g2d.fillRect(x - (width / 2), y + height - 4, width, 4);
		}
	}

	// Getters and Setters for aiming adjustments
	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getBarrelAngle() {
		return barrelAngle;
	}

	public void changeAngle(int amount) {
		this.barrelAngle += amount;
		// Keep angle between 0 and 180 degrees
		if (barrelAngle > 180)
			barrelAngle = 180;
		else if (barrelAngle < 0)
			barrelAngle = 0;
		else
			SoundEngine.playBarrelRotateSound();
	}

	public double getPower() {
		return power;
	}

	public void changePower(double amount) {
		this.power += amount;
		// Keep power between 1 and 25
		if (this.power < 1.0)
			this.power = 1.0;
		else if (this.power > 25.0)
			this.power = 25.0;
		else
			SoundEngine.playPowerChargeSound(power);
	}

	public boolean isAlive() {
		return alive;
	}

	public int getHealth() {
		return currentHealth;
	}

	/**
	 * Calculates damage based on proximity to an explosion center.
	 */
	public void takeDamage(int damage) {
		if (!alive)
			return;

		this.currentHealth -= damage;
		
		// Kill tank
		if (this.currentHealth <= 0) {
			this.currentHealth = 0;
			this.alive = false;
			SoundEngine.playTankDeathSound();
			
			// Create destroyed turret and pass to listener
			TurretDebris poppedTurret = new TurretDebris(this.x, this.y, this.barrelAngle);
			if (this.damageListener != null) {
		        this.damageListener.onTurretSpawned(poppedTurret);
		    }
		}
	}

	/**
	 * Reset health for new rounds if needed.
	 */
	public void reset() {
		this.currentHealth = maxHealth;
		this.alive = true;
	}

	/**
	 * Checks if a specific coordinate point hits this tank's physical body.
	 */
	public boolean checkHit(double px, double py) {
		if (!alive)
			return false;

		// Calculate the boundary box matching the tank body:
		// g2d.fillRect(x - (width / 2), y, width, height);
		int leftBound = this.x - (this.width / 2);
		int rightBound = this.x + (this.width / 2);
		int topBound = this.y;
		int bottomBound = this.y + this.height;

		// Return true if the point falls completely inside these 4 walls
		return (px >= leftBound && px <= rightBound && py >= topBound && py <= bottomBound);
	}

	public Color getColor() {
		return this.color;
	}
	
	/**
	 * Attaches a listener to hear about damage events.
	 */
	public void setDamageListener(DamageListener listener) {
	    this.damageListener = listener;
	}
}