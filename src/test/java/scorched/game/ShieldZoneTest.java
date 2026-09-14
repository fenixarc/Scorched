package scorched.game;

import static org.junit.Assert.*;
import org.junit.Test;
import java.awt.Color;

public class ShieldZoneTest {

    @Test
    public void testShieldAbsorptionAndDestruction() {
        // Mocking a tank
        Player p = new Player();
        p.setPlayerName("TestTank");
        Terrain terrain = new Terrain(800, 600, Color.BLACK, 1);
        Tank tank = new Tank(p, 100, terrain, Color.RED, 45, 0);
        
        ShieldZone shield = new ShieldZone(tank, 25, 0);
        
        assertFalse("Shield should be active initially", shield.isDestroyed());
        
        shield.absorbDamage(30);
        assertFalse("Shield should still be active after 30 damage", shield.isDestroyed());
        
        shield.absorbDamage(20);
        assertTrue("Shield should be destroyed after 50 total damage", shield.isDestroyed());
    }

    @Test
    public void testShieldOwner() {
        Player p = new Player();
        p.setPlayerName("TestTank");
        Terrain terrain = new Terrain(800, 600, Color.BLACK, 1);
        Tank tank = new Tank(p, 100, terrain, Color.RED, 45, 0);
        
        ShieldZone shield = new ShieldZone(tank, 25, 0);
        assertEquals("Shield owner should match the tank", tank, shield.getOwner());
    }
}
