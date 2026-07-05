package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectileSimulatorTest {

    @Mock
    private Terrain mockTerrain;

    @Mock
    private Tank mockTarget;

    @BeforeEach
    void setUp() {
        // Default setup: a standard 800px wide screen
        lenient().when(mockTerrain.getScreenWidth()).thenReturn(800);
        // Default setup: terrain is completely empty/hollow by default
        lenient().when(mockTerrain.isSolidAt(anyDouble(), anyDouble())).thenReturn(false);
    }

    @Test
    void testDirectHit_TargetRightInFront() {
        // Arrange: Place target directly in the horizontal path of the shot
        // With an angle of 0 and power of 10, x increases by ~1.0 per step (10 * 0.1).
        when(mockTarget.getX()).thenReturn(50);
        when(mockTarget.getY()).thenReturn(100);

        // Act
        boolean result = ProjectileSimulator.checkTrajectory(0, 100, 0, 10, mockTerrain, mockTarget);

        // Assert
        assertTrue(result, "Projectile should have hit the target directly.");
    }

    @Test
    void testMiss_OutOfBoundsLeft() {
        // Arrange: Shooting backward (180 degrees) out of bounds instantly
        when(mockTarget.getX()).thenReturn(400);
        when(mockTarget.getY()).thenReturn(300);

        // Act
        boolean result = ProjectileSimulator.checkTrajectory(5, 100, 180, 10, mockTerrain, mockTarget);

        // Assert
        assertFalse(result, "Projectile should immediately go out of bounds on the left.");
    }

    @Test
    void testMiss_OutOfBoundsRight() {
        // Arrange: Shooting off the right edge of the screen
        when(mockTarget.getX()).thenReturn(400);
        when(mockTarget.getY()).thenReturn(300);

        // Act
        boolean result = ProjectileSimulator.checkTrajectory(795, 100, 0, 10, mockTerrain, mockTarget);

        // Assert
        assertFalse(result, "Projectile should go out of bounds on the right side.");
    }

    @Test
    void testMiss_TerrainBlocker() {
        // Arrange: Target is far away, but there is solid terrain right in front of the launcher
        when(mockTarget.getX()).thenReturn(500);
        when(mockTarget.getY()).thenReturn(100);
        
        // Mock a mountain covering the x range around 50.0 to prevent floating-point mismatches
        when(mockTerrain.isSolidAt(anyDouble(), anyDouble())).thenAnswer(invocation -> {
            double x = invocation.getArgument(0);
            return x >= 45.0 && x <= 55.0;
        });

        // Act
        boolean result = ProjectileSimulator.checkTrajectory(40, 100, 0, 10, mockTerrain, mockTarget);

        // Assert
        assertFalse(result, "Projectile should have been blocked by the terrain before reaching the target.");
    }

    @Test
    void testHit_TerrainAndTargetIntersect() {
        // Arrange: The projectile hits terrain, BUT it's close enough to the target to count as a hit anyway
        when(mockTarget.getX()).thenReturn(105);
        when(mockTarget.getY()).thenReturn(100);
        
        // Solid terrain right where the target is
        when(mockTerrain.isSolidAt(anyDouble(), anyDouble())).thenAnswer(invocation -> {
            double x = invocation.getArgument(0);
            return x >= 100.0; 
        });

        // Act
        boolean result = ProjectileSimulator.checkTrajectory(0, 100, 0, 20, mockTerrain, mockTarget);

        // Assert
        assertTrue(result, "Projectile should return true if it hits terrain that is within the target radius.");
    }

    @Test
    void testMiss_Timeout() {
        // Arrange: Shoot straight up with high power so it flies for a long time and never hits anything
        when(mockTarget.getX()).thenReturn(400);
        when(mockTarget.getY()).thenReturn(500);

        // Act
        boolean result = ProjectileSimulator.checkTrajectory(100, 400, 90, 50, mockTerrain, mockTarget);

        // Assert
        assertFalse(result, "Projectile should return false after the 100-second simulation limit expires.");
    }
}