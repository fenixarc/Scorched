package scorched.game;

import static org.junit.Assert.*;
import org.junit.Test;
import java.awt.Graphics2D;

public class EffectZoneTest {

    private static class TestEffectZone extends EffectZone {
        public TestEffectZone(double x, double y, int radius, int turnsRemaining, int damagePerTurn, String type) {
            super(x, y, radius, turnsRemaining, damagePerTurn, type);
        }

        @Override
        public void draw(Graphics2D g) {
            // No-op for testing
        }
    }

    @Test
    public void testDecrementTurn() {
        TestEffectZone zone = new TestEffectZone(0, 0, 10, 2, 5, "test");
        
        zone.decrementTurn();
        assertEquals(1, zone.turnsRemaining);
        assertFalse(zone.isExpired());
        
        zone.decrementTurn();
        assertEquals(0, zone.turnsRemaining);
        assertTrue(zone.isExpired());
        
        zone.decrementTurn();
        assertEquals(0, zone.turnsRemaining);
        assertTrue(zone.isExpired());
    }

    @Test
    public void testContains() {
        TestEffectZone zone = new TestEffectZone(100, 100, 50, 1, 0, "test");
        
        assertTrue("Point inside radius should be contained", zone.contains(100, 100));
        assertTrue("Point on boundary should be contained", zone.contains(150, 100));
        assertFalse("Point outside radius should not be contained", zone.contains(200, 100));
    }
}
