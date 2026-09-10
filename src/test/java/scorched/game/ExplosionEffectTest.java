package scorched.game;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ExplosionEffectTest {

    private ExplosionEffect explosionEffect;
    private final int centerX = 120;
    private final int centerY = 180;
    private final int radius = 100;

    @BeforeEach
    public void setUp() {
        explosionEffect = new ExplosionEffect(centerX, centerY, radius);
    }

    @Test
    public void testInitializationActive() {
        assertTrue(explosionEffect.isActive(), "ExplosionEffect should be active immediately upon creation.");
    }

    @Test
    public void testUpdateMaintainsActiveStateWithinDuration() {
        explosionEffect.update();
        assertTrue(explosionEffect.isActive(), "ExplosionEffect should remain active shortly after creation.");
    }

    @Test
    public void testDeactivatesAfterDuration() throws InterruptedException {
        Thread.sleep(1050); // Wait past DURATION_MS (1000ms)
        explosionEffect.update();
        assertFalse(explosionEffect.isActive(), "ExplosionEffect should become inactive after 1000ms duration.");
    }

    @Test
    public void testDrawDoesNotThrow() {
        Graphics2D mockG2d = Mockito.mock(Graphics2D.class);
        assertDoesNotThrow(() -> explosionEffect.draw(mockG2d));

        explosionEffect.update();
        assertDoesNotThrow(() -> explosionEffect.draw(mockG2d));
    }
}
