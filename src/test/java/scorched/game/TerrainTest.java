package scorched.game;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TerrainTest {

    private Terrain terrain;
    private final int WIDTH = 100;
    private final int HEIGHT = 400;
    private final Color DIRT_COLOR = new Color(139, 69, 19); // Brown
    private final int STANDARD_HILL_STRENGTH = 1;

    @BeforeEach
    public void setUp() {
        // Updated to include the hillStrength parameter required by the constructor
        terrain = new Terrain(WIDTH, HEIGHT, DIRT_COLOR, STANDARD_HILL_STRENGTH);
    }

    @Test
    public void testTerrainGenerationBoundaries() {
        // Verify that the generated terrain respects the new hardcoded limits in generateTerrain()
        // minY = 60, maxY = screenHeight - 40 (400 - 40 = 360)
        int minY = 60;
        int maxY = HEIGHT - 40;

        for (int x = 0; x < WIDTH; x++) {
            int surfaceY = terrain.getHeightAt(x);
            
            // Updated to match the new reflection bounds and Math.clamp safety checks
            assertTrue(surfaceY >= minY, "Terrain surface should not be higher than Y=" + minY);
            assertTrue(surfaceY <= maxY, "Terrain surface should not be lower than Y=" + maxY);
        }
    }

    @Test
    public void testGetHeightAtBoundsHandling() {
        // The method should safely clamp negative x values to 0
        int negativeXHeight = terrain.getHeightAt(-50);
        int zeroXHeight = terrain.getHeightAt(0);
        assertEquals(zeroXHeight, negativeXHeight, "Negative X should clamp to 0");

        // The method should safely clamp out-of-bounds x values to screenWidth - 1
        int overBoundsXHeight = terrain.getHeightAt(WIDTH + 50);
        int maxXHeight = terrain.getHeightAt(WIDTH - 1);
        assertEquals(maxXHeight, overBoundsXHeight, "Out-of-bounds X should clamp to screenWidth - 1");
    }

    @Test
    public void testExplodeRemovesTerrain() {
        int targetX = 50;
        // Find the natural surface height at this column
        int surfaceY = terrain.getHeightAt(targetX);
        
        // Target an explosion right on the surface to clear a chunk out
        int radius = 10;
        terrain.explode(targetX, surfaceY + radius, radius);

        // After the explosion, the highest solid point at targetX should be lower down (higher Y value)
        int newSurfaceY = terrain.getHeightAt(targetX);
        assertTrue(newSurfaceY > surfaceY, "Explosion should have lowered the terrain surface");
    }

    @Test
    public void testExplodeEdgeCases() {
        // Ensure explosions at negative or out-of-bounds coordinates don't throw an exception
        assertDoesNotThrow(() -> terrain.explode(-10, -10, 5));
        assertDoesNotThrow(() -> terrain.explode(WIDTH + 10, HEIGHT + 10, 5));
    }

    @Test
    public void testUpdateTriggersGravityAndSettles() {
        int targetX = 50;
        int surfaceY = terrain.getHeightAt(targetX);
        int radius = 15;

        // 1. Create a cave/floating shelf by exploding a circle completely below the surface
        // This leaves a "ceiling" of dirt hanging in the air
        int explosionCenterY = surfaceY + radius + 10; 
        terrain.explode(targetX, explosionCenterY, radius);

        // 2. The update() loop should detect floating terrain and return true (terrain moved)
        boolean terrainMoved = terrain.update();
        assertTrue(terrainMoved, "Terrain should actively fall due to gravity after a floating shelf is created");

        // 3. Loop the update until the terrain completely settles (returns false)
        int safetyCounter = 0;
        while (terrain.update() && safetyCounter < HEIGHT) {
            safetyCounter++;
        }

        // Verify it didn't get stuck in an infinite loop
        assertTrue(safetyCounter < HEIGHT, "Terrain physics should eventually stabilize");

        // 4. Now that it is stable, calling update() again must return false
        assertFalse(terrain.update(), "Stable terrain should return false on update");
    }

    @Test
    public void testDifferentHillStrengthsDoNotCrash() {
        // Extra test case to verify that higher tier hill strengths do not throw exceptions
        assertDoesNotThrow(() -> new Terrain(WIDTH, HEIGHT, DIRT_COLOR, 2));
        assertDoesNotThrow(() -> new Terrain(WIDTH, HEIGHT, DIRT_COLOR, 3));
    }
}