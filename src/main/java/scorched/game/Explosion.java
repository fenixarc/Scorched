package scorched.game;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a visual particle explosion effect in the game.
 *
 * Generates a cluster of Particles to simulate an explosion.
 * The class manages the lifecycle, updating, and rendering of its particles.
 * 
 */
public class Explosion {
    private List<Particle> particles;
    private boolean active = true;

    public Explosion(int centerX, int centerY) {
        particles = new ArrayList<>();
        Random rand = new Random();
        
        // Set explosion colors
        Color[] fireColors = { Color.RED, Color.ORANGE, Color.YELLOW, Color.WHITE };

        // Spawn a burst cluster of 45 particles per blast
        for (int i = 0; i < 45; i++) {
            // Generate a random angle and explosion push speed
            double angle = rand.nextDouble() * 2.0 * Math.PI;
            double speed = 1.0 + rand.nextDouble() * 5.0; // Dynamic force
            
            double vx = Math.cos(angle) * speed;
            // Subtract extra upward velocity to simulate a blast erupting out of the ground
            double vy = (Math.sin(angle) * speed) - 2.0; 

            Color randomColor = fireColors[rand.nextInt(fireColors.length)];
            int randomLife = 20 + rand.nextInt(25); // Lifespan between 20-45 frames

            particles.add(new Particle(centerX, centerY, vx, vy, randomColor, randomLife));
        }
    }

    public boolean isActive() { return active; }

    /**
     * Loops through particles, updates them, and culls dead ones.
     */
    public void update() {
        if (!active) return;

        boolean anyAlive = false;
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if (!p.update()) {
                particles.remove(i); // Delete dead particles from RAM
            } else {
                anyAlive = true;
            }
        }

        // If all contained particles have dissolved to pure black transparency, kill the event
        if (!anyAlive) {
            active = false;
        }
    }

    public void draw(Graphics2D g2d) {
        if (!active) return;
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }
}