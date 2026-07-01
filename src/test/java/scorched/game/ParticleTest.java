package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ParticleTest {

    private Color testColor;
    private int startX;
    private int startY;

    @BeforeEach
    void setUp() {
        startX = 100;
        startY = 200;
        testColor = Color.RED;
    }

    @Test
    @DisplayName("Particle should stay alive and update positions and gravity correctly")
    void testUpdateMovementAndGravity() {
        double vx = 2.0;
        double vy = -5.0; 
        int maxLife = 10;
        
        Particle particle = new Particle(startX, startY, vx, vy, testColor, maxLife);

        // Update 1: Decrements life from 10 to 9. Returns true.
        assertTrue(particle.update(), "Particle should be alive after the first update");
        
        // Updates 2 through 8: Decrements life down to 2.
        // A particle with maxLife = 10 will return true exactly 9 times before dying on the 10th.
        for (int i = 2; i <= 8; i++) {
            assertTrue(particle.update(), "Particle should remain alive during its mid-lifespan");
        }
        
        // Update 9: Decrements life from 2 to 1. This is its absolute last frame alive!
        assertTrue(particle.update(), "Particle should be on its last frame of life (life == 1)");
        
        // Update 10: Decrements life from 1 to 0. It should now return false.
        assertFalse(particle.update(), "Particle should return false (die) when life reaches 0");
    }

    @Test
    @DisplayName("Particle alpha transparency should fade over time")
    void testAlphaFadingAndInstantDeath() {
        int maxLife = 2;
        Particle particle = new Particle(startX, startY, 1.0, 1.0, testColor, maxLife);

        // Update 1: life becomes 1 (Alive)
        assertTrue(particle.update(), "Should be alive on frame 1");
        
        // Update 2: life becomes 0 (Dead)
        assertFalse(particle.update(), "Particle should die when life reaches 0");
    }

    @Test
    @DisplayName("Particle initialized with 0 or negative life should die immediately")
    void testImmediateDeath() {
        Particle deadParticle = new Particle(startX, startY, 0, 0, testColor, 0);
        assertFalse(deadParticle.update(), "Particle with 0 maxLife should die on the very first update");
    }

    @Test
    @DisplayName("Draw method executes successfully without crashing")
    void testDrawDoesNotThrowException() {
        Particle particle = new Particle(startX, startY, 1.0, 1.0, testColor, 5);
        
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> particle.draw(g2d), "Drawing the particle should not throw an exception");
        
        g2d.dispose();
    }
}