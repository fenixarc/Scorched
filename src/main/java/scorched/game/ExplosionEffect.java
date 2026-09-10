package scorched.game;

import java.awt.Color;
import java.awt.Graphics2D;

public class ExplosionEffect {
    private final int centerX;
    private final int centerY;
    private final int explosionRadius;
    private final long startTime;
    private boolean active;
    private static final long DURATION_MS = 1000; // 1 second

    private int currentRadius = 1;

    // Color gradient bounds (center to outer edge)
    private static final int CENTER_RED = 255;
    private static final int EDGE_RED = 60; // Dark crimson

    public ExplosionEffect(int centerX, int centerY, int explosionRadius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.explosionRadius = explosionRadius;
        this.startTime = System.currentTimeMillis();
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void update() {
        if (!active) return;

        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= DURATION_MS) {
            active = false;
            return;
        }

        double progress = (double) elapsedTime / DURATION_MS;
        this.currentRadius = Math.max(1, (int) (1 + (explosionRadius - 1) * progress));
    }

    public void draw(Graphics2D g2d) {
        if (!active || currentRadius <= 0) return;

        // Draw concentric opaque rings from outer current radius inward to center
        for (int r = currentRadius; r >= 1; r--) {
            double ratio = (double) r / explosionRadius;
            if (ratio > 1.0) ratio = 1.0;

            // Interpolate red component from bright pure red (255, 0, 0) at center to dark crimson (60, 0, 0) at edge
            int red = (int) (CENTER_RED - ratio * (CENTER_RED - EDGE_RED));
            red = Math.max(0, Math.min(255, red));

            g2d.setColor(new Color(red, 0, 0));
            g2d.fillOval(centerX - r, centerY - r, r * 2, r * 2);
        }
    }
}
