package scorched.game;
import java.awt.Color;
import java.awt.Graphics2D;

public class Particle {
    private double x, y;
    private double vx, vy;
    private Color color;
    private int alpha = 255; // Transparency (255 = fully visible, 0 = invisible)
    private int life;        // Remaining frames to live
    private int maxLife;

    public Particle(int startX, int startY, double vx, double vy, Color color, int maxLife) {
        this.x = startX;
        this.y = startY;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.maxLife = maxLife;
        this.life = maxLife;
    }

    /**
     * Updates physics tracking and fades transparency frame by frame.
     */
    public boolean update() {
        life--;
        if (life <= 0) return false; // Particle is dead

        // Apply velocities
        x += vx;
        y += vy;

        // Apply a little gravity pull to make sparks arc gracefully downward
        vy += 0.15;

        // Calculate fade percentage
        alpha = (int) (((double) life / maxLife) * 255);
        if (alpha < 0) alpha = 0;

        return true; // Particle is still alive
    }

    public void draw(Graphics2D g2d) {
        // Create a copy of the color with our calculated alpha transparency fade
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        
        // Draw the particle as a tiny retro square pixel block
        int size = 3 + (int)(life * 0.1); // Make them shrink slightly over time
        g2d.fillRect((int) x, (int) y, size, size);
    }
}