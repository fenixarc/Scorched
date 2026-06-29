package com.scorched;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class FloatingText {
    private double x, y;
    private double vy; // Upward speed
    private String text;
    private Color color;
    private int life; // Frames to live
    private int maxLife;

    public FloatingText(int startX, int startY, String text, Color color, int maxLife) {
        this.x = startX;
        // Spawn slightly above the tank hull so it doesn't overlap text stats
        this.y = startY - 20; 
        this.text = text;
        this.color = color;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.vy = -1.2; // Move upward at 1.2 pixels per frame
    }

    /**
     * Drifts the text upward. Returns false when its life expires.
     */
    public boolean update() {
        life--;
        if (life <= 0) return false;

        y += vy; // Apply upward drift
        return true;
    }

    public void draw(Graphics2D g2d) {
        // Calculate transparency percentage (fade out towards the end of life)
        float alpha = (float) life / maxLife;
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;

        // Save original graphics state
        java.awt.Composite originalComposite = g2d.getComposite();
        Font originalFont = g2d.getFont();
        
        // Set alpha rendering transparency
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        
        // Draw drop shadow for crisp readability against any background sky color
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, (int) x + 1, (int) y + 1);
        
        // Draw main colored text
        g2d.setColor(color);
        g2d.drawString(text, (int) x, (int) y);

        // Restore original graphics state
        g2d.setComposite(originalComposite);
        g2d.setFont(originalFont);
    }
}