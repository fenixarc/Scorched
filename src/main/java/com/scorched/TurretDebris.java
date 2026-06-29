package com.scorched;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class TurretDebris {
    private double x, y;
    private double vx, vy;
    private double angle;         // Current rotation angle in degrees
    private double rotationSpeed; // How fast it spins mid-air
    private boolean active = true;

    public TurretDebris(int startX, int startY, int initialAngle) {
        this.x = startX;
        this.y = startY - 5; // Spawn slightly above the hull base
        this.angle = initialAngle;

        // Blast vectors: Send it flying upwards and slightly randomized horizontally
        java.util.Random rand = new java.util.Random();
        this.vx = (rand.nextDouble() * 4.0) - 2.0;  // Random drift left/right
        this.vy = -(4.0 + rand.nextDouble() * 5.0); // Strong pop upward (-Y is up)
        
        // Spin metrics: Random clockwise or counter-clockwise tumbling rotation
        this.rotationSpeed = (rand.nextBoolean() ? 8.0 : -8.0) + (rand.nextDouble() * 4.0);
    }

    public boolean isActive() { return active; }

    /**
     * Updates falling physics and rotation. Crashes when it hits the soil.
     */
    public void update(Terrain terrain, int screenWidth, int screenHeight) {
        if (!active) return;

        // Apply basic gravity acceleration
        double gravity = 0.25;
        vy += gravity;

        x += vx;
        y += vy;
        angle += rotationSpeed; // Tumble

        // Bounds checks
        if (x < 0 || x >= screenWidth || y >= screenHeight) {
            active = false;
            return;
        }

        // Terrain Collision Check
        if (y >= 0 && x >= 0 && x < screenWidth) {
            int groundY = terrain.getHeightAt((int) x);
            if (y >= groundY) {
                // Stop when it hits ground
                y = groundY;
                active = false; 
            }
        }
    }

    public void draw(Graphics2D g2d) {
        // Save the graphics coordinate matrix layer context
        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();

        // Move the drawing origin brush directly to the debris pixel spot
        g2d.translate(x, y);
        g2d.rotate(Math.toRadians(angle));

        // Draw the detached turret barrel line relative to its new tumbling local origin (0,0)
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3.0f)); // Thick barrel matching tank style
        g2d.drawLine(0, 0, 16, 0); // 16 pixels long barrel

        // Restore original drawing canvas orientations
        g2d.setTransform(oldTransform);
    }
}