package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import scorched.sound.SoundEngine;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankTest {

    private Terrain mockTerrain;
    private DamageListener mockDamageListener;
    private final Color tankColor = Color.GREEN;
    private final int playerIndex = 1;

    @BeforeEach
    void setUp() {
        mockTerrain = mock(Terrain.class);
        mockDamageListener = mock(DamageListener.class);
        
        // Default terrain behavior: flat ground at Y = 200
        when(mockTerrain.getHeightAt(anyInt())).thenReturn(200);
    }

    @Test
    void testConstructorSnapsToTerrainAndInitializesAI() {
        // Tank height is 14. Terrain is 200. Expected Y = 200 - 14 = 186
        // Passing aiLevel = 1 to verify AI initialization
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 1);

        assertEquals(100, tank.getX());
        assertEquals(186, tank.getY());
        assertEquals(90, tank.getBarrelAngle());
        assertEquals(tankColor, tank.getColor());
        assertEquals(playerIndex, tank.getPlayerIndex());
        assertTrue(tank.isAlive());
        assertEquals(100, tank.getHealth());
        assertNotNull(tank.getAI());
    }

    @Test
    void testConstructorWithoutAI() {
        // Passing aiLevel = 0 should leave AI as null
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);
        assertNull(tank.getAI());
    }

    @Test
    void testChangeAngleClamping() {
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);

        try (MockedStatic<SoundEngine> mockedSoundEngine = mockStatic(SoundEngine.class)) {
            // Test normal adjustments
            tank.changeAngle(45);
            assertEquals(135, tank.getBarrelAngle());
            mockedSoundEngine.verify(SoundEngine::playBarrelRotateSound, times(1));

            // Test upper clamping (max 180)
            tank.changeAngle(100);
            assertEquals(180, tank.getBarrelAngle());

            // Test lower clamping (min 0)
            tank.changeAngle(-200);
            assertEquals(0, tank.getBarrelAngle());
        }
    }

    @Test
    void testSetBarrelAngle() {
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);

        tank.setBarrelAngle(120);
        assertEquals(120, tank.getBarrelAngle());

        tank.setBarrelAngle(250); // Clamped to 180
        assertEquals(180, tank.getBarrelAngle());

        tank.setBarrelAngle(-50); // Clamped to 0
        assertEquals(0, tank.getBarrelAngle());
    }

    @Test
    void testChangePowerClamping() {
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);
        assertEquals(10.0, tank.getPower());

        try (MockedStatic<SoundEngine> mockedSoundEngine = mockStatic(SoundEngine.class)) {
            // Test normal variation
            tank.changePower(5.5);
            assertEquals(15.5, tank.getPower());
            mockedSoundEngine.verify(() -> SoundEngine.playPowerChargeSound(15.5), times(1));

            // Test upper bound clamping (updated to match MAX_POWER = 50.0)
            tank.changePower(40.0);
            assertEquals(50.0, tank.getPower());

            // Test lower bound clamping (min 1.0)
            tank.changePower(-60.0);
            assertEquals(1.0, tank.getPower());
        }
    }

    @Test
    void testSetPower() {
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);

        tank.setPower(18.5);
        assertEquals(18.5, tank.getPower());

        tank.setPower(60.0); // Clamped to MAX_POWER (50.0)
        assertEquals(50.0, tank.getPower());

        tank.setPower(0.5); // Clamped to MIN_POWER (1.0)
        assertEquals(1.0, tank.getPower());
    }

    @Test
    void testCheckHit() {
        // Tank is at X=100, Y=186. Width=30 (X bounds: 85 to 115). Height=14 (Y bounds: 186 to 200)
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);

        // Center hit
        assertTrue(tank.checkHit(100, 190));
        // Boundary edges
        assertTrue(tank.checkHit(85, 186));
        assertTrue(tank.checkHit(115, 200));

        // Misses
        assertFalse(tank.checkHit(84, 190));  // Too far left
        assertFalse(tank.checkHit(116, 190)); // Too far right
        assertFalse(tank.checkHit(100, 185)); // Too high
        assertFalse(tank.checkHit(100, 201)); // Too low
    }

    @Test
    void testApplyGravity_WhileFallingAndLanding() {
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);
        tank.setDamageListener(mockDamageListener);

        // Suddenly change terrain height drastically downward so the tank is in mid-air
        // Old target Y was 186. New target Y will be 300 - 14 = 286.
        when(mockTerrain.getHeightAt(100)).thenReturn(300);

        // First frame of fall: should apply gravity drop speed (+4 Y) and return true
        boolean isFalling = tank.applyGravity(mockTerrain);
        assertTrue(isFalling);
        assertEquals(190, tank.getY()); // 186 + 4

        try (MockedStatic<SoundEngine> mockedSoundEngine = mockStatic(SoundEngine.class)) {
            // Fall loop: keep ticking while applyGravity returns true
            int safetyCounter = 0;
            while (tank.applyGravity(mockTerrain) && safetyCounter < 50) {
                safetyCounter++;
            }
            
            // Now that the loop has exited, applyGravity returned false. 
            assertFalse(tank.applyGravity(mockTerrain)); 
            assertEquals(286, tank.getY()); // Snapped exactly to ground
            
            // Verify sound played
            mockedSoundEngine.verify(SoundEngine::playFallDamageSound, times(1));
        }
        
        // Assert damage application
        // Initial fallStartY was 186. Landed at 286. Total drop = 100 pixels.
        // excessiveFall = 100 - 20 (safe distance) = 80. fallDamage = 80 / 2 = 40.
        assertEquals(60, tank.getHealth()); // 100 - 40 damage
        verify(mockDamageListener).onTankTakeDamage(eq(100), eq(286), eq(40));
    }

    @Test
    void testTakeDamageAndDeath() {
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);
        tank.setDamageListener(mockDamageListener);

        try (MockedStatic<SoundEngine> mockedSoundEngine = mockStatic(SoundEngine.class)) {
            
            // Take partial damage
            tank.takeDamage(40);
            assertEquals(60, tank.getHealth());
            assertTrue(tank.isAlive());

            // Take lethal damage
            tank.takeDamage(70);
            assertEquals(0, tank.getHealth());
            assertFalse(tank.isAlive());

            // Verify static death sound was played
            mockedSoundEngine.verify(SoundEngine::playTankDeathSound, times(1));
            
            // Verify turret debris callback was triggered
            verify(mockDamageListener).onTurretSpawned(any(TurretDebris.class));
            
            // Post-death damage should do nothing
            tank.takeDamage(10);
            assertEquals(0, tank.getHealth());
        }
    }

    @Test
    void testReset() {
        Tank tank = new Tank(100, mockTerrain, tankColor, 90, playerIndex, 0);
        
        try (MockedStatic<SoundEngine> mockedSoundEngine = mockStatic(SoundEngine.class)) {
            tank.takeDamage(100);
            assertFalse(tank.isAlive());

            tank.reset();
            assertTrue(tank.isAlive());
            assertEquals(100, tank.getHealth());
        }
    }
}