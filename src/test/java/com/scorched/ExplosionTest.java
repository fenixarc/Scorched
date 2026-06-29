package com.scorched;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.Graphics2D;

public class ExplosionTest {

    private Explosion explosion;
    private final int centerX = 100;
    private final int centerY = 150;

    @BeforeEach
    public void setUp() {
        // Initialize a fresh explosion before each test
        explosion = new Explosion(centerX, centerY);
    }

    @Test
    public void testInitializationActive() {
        // Assert that a newly created explosion is immediately active
        assertTrue(explosion.isActive(), "Explosion should be active upon creation.");
    }

    @Test
    public void testUpdateMaintainsActiveStateInitially() {
        // The minimum lifespan of a particle is 20 frames. 
        // Updating 5 times should definitely keep the explosion active.
        for (int i = 0; i < 5; i++) {
            explosion.update();
        }
        assertTrue(explosion.isActive(), "Explosion should still be active after only 5 updates.");
    }

    @Test
    public void testExplosionDeactivatesAfterMaxLifespan() {
        // The maximum lifespan of a particle is 45 frames (20 + random 25).
        // Updating 50 times guarantees all particles will have reached 0 life.
        for (int i = 0; i < 50; i++) {
            explosion.update();
        }

        assertFalse(explosion.isActive(), "Explosion should be inactive after all particles exceed maximum lifespan.");
    }

    @Test
    public void testUpdateDoesNothingWhenAlreadyInactive() {
        // Force the explosion to become inactive by aging it out
        for (int i = 0; i < 50; i++) {
            explosion.update();
        }
        assertFalse(explosion.isActive());

        // Call update again; it should hit the `if (!active) return;` guard clause safely
        assertDoesNotThrow(() -> explosion.update(), "Calling update on an inactive explosion should not throw exceptions.");
    }

    @Test
    public void testDrawDoesNotThrowException() {
        // Create a mock Graphics2D object using Mockito to test the draw routine
        Graphics2D mockG2d = Mockito.mock(Graphics2D.class);
        
        // Ensure drawing a live explosion doesn't crash
        assertDoesNotThrow(() -> explosion.draw(mockG2d), "Drawing active explosion should run cleanly.");
        
        // Age it out to inactive
        for (int i = 0; i < 50; i++) {
            explosion.update();
        }
        
        // Ensure drawing an inactive explosion safely hits the early return guard clause
        assertDoesNotThrow(() -> explosion.draw(mockG2d), "Drawing inactive explosion should run cleanly.");
    }
}