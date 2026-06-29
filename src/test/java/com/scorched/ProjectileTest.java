package com.scorched;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectileTest {

    private Terrain mockTerrain;
    private Tank mockTank;
    private List<Tank> tankList;
    private MockedStatic<SoundEngine> mockedSoundEngine;

    @BeforeEach
    void setUp() {
        // Mock dependencies
        mockTerrain = mock(Terrain.class);
        mockTank = mock(Tank.class);
        tankList = new ArrayList<>();
        tankList.add(mockTank);

        // Stub out static SoundEngine calls to prevent real audio execution during tests
        mockedSoundEngine = mockStatic(SoundEngine.class);
    }

    @AfterEach
    void tearDown() {
        // Deregister the static mock after every test case
        mockedSoundEngine.close();
    }

    @Test
    void testInitializationAndGetters() {
        // Fire at 45 degrees, power 10
        Projectile projectile = new Projectile(100, 200, 45, 10.0);

        assertTrue(projectile.isActive());
        assertEquals(40, projectile.getExplosionRadius());
        assertEquals(60, projectile.getDamage());
    }

    @Test
    void testUpdateMovesProjectileAndAppliesGravity() {
        // Firing straight right (0 degrees) with 10 power
        // vx = 10, vy = 0 initially. After 1 update: vy = 0.15 (gravity), x = 110, y = 200.15
        Projectile projectile = new Projectile(100, 200, 0, 10.0);
        
        // Setup terrain so it doesn't collide (ground is deep down)
        when(mockTerrain.getHeightAt(anyInt())).thenReturn(500);
        when(mockTank.isAlive()).thenReturn(false);

        projectile.update(mockTerrain, tankList, 800, 600);

        assertTrue(projectile.isActive(), "Projectile should still be active");
    }

    @Test
    void testOffScreenDeactivatesProjectile() {
        // Fire right, close to the edge of the screen width (800)
        Projectile projectile = new Projectile(795, 200, 0, 10.0);
        
        when(mockTerrain.getHeightAt(anyInt())).thenReturn(500);

        // First update moves X to 805, which is >= screenWidth (800)
        projectile.update(mockTerrain, tankList, 800, 600);

        assertFalse(projectile.isActive(), "Projectile should deactivate when moving off-screen horizontally");
    }

    @Test
    void testSkyCheckDoesNotDeactivate() {
        // Firing straight up (90 degrees), power 10. vy becomes negative (shooting up)
        // From y=5, it will go to y < 0 (above the screen)
        Projectile projectile = new Projectile(100, 5, 90, 10.0);
        
        when(mockTerrain.getHeightAt(anyInt())).thenReturn(500);

        projectile.update(mockTerrain, tankList, 800, 600);

        // Projectile goes above y=0, but should stay active according to requirements
        assertTrue(projectile.isActive(), "Projectile should stay active while soaring through the sky (y < 0)");
    }

    @Test
    void testDirectHitOnTank() {
        Projectile projectile = new Projectile(100, 200, 0, 10.0);

        // Setup Tank to trigger a successful hit
        when(mockTank.isAlive()).thenReturn(true);
        when(mockTank.checkHit(anyDouble(), anyDouble())).thenReturn(true);

        projectile.update(mockTerrain, tankList, 800, 600);

        assertFalse(projectile.isActive(), "Projectile should deactivate upon hitting a tank");
        
        // Check impact coordinates are updated (100 + approx 10 from vx)
        assertTrue(projectile.getImpactX() > 100); 
        
        // Verify external explosions and sounds were triggered
        verify(mockTerrain).explode(eq(projectile.getImpactX()), eq(projectile.getImpactY()), eq(projectile.getExplosionRadius()));
        mockedSoundEngine.verify(SoundEngine::playExplosionSound, times(1));
    }

    @Test
    void testTerrainCollision() {
        // Projectile is right above the ground (y = 295, terrain height = 300)
        Projectile projectile = new Projectile(100, 295, 0, 10.0);

        when(mockTank.isAlive()).thenReturn(false);
        // Force the terrain to be higher than the next projectile Y position
        when(mockTerrain.getHeightAt(anyInt())).thenReturn(290);

        projectile.update(mockTerrain, tankList, 800, 600);

        assertFalse(projectile.isActive(), "Projectile should deactivate upon hitting the terrain ground");
        verify(mockTerrain).explode(anyInt(), anyInt(), eq(projectile.getExplosionRadius()));
        mockedSoundEngine.verify(SoundEngine::playExplosionSound, times(1));
    }

    @Test
    void testInactiveProjectileDoesNotUpdate() {
        Projectile projectile = new Projectile(100, 200, 0, 10.0);
        
        // Deactivate it via an offscreen movement
        projectile.update(mockTerrain, tankList, 50, 600); 
        assertFalse(projectile.isActive());

        // Reset mock invocations to check if methods are called afterward
        Mockito.reset(mockTerrain);

        // Attempting to update an inactive projectile should immediately return
        projectile.update(mockTerrain, tankList, 800, 600);
        
        verifyNoInteractions(mockTerrain);
    }
}