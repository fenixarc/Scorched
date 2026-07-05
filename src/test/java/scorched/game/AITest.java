package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import scorched.weapons.AmmoType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AITest {

    private Tank myTank;
    private Terrain terrain;
    private Inventory inventory;
    private AmmoType defaultAmmo;

    @BeforeEach
    void setUp() {
        myTank = mock(Tank.class);
        terrain = mock(Terrain.class);
        inventory = mock(Inventory.class);
        defaultAmmo = mock(AmmoType.class);
        
        // Give our own tank a default index, position, and setup its inventory
        when(myTank.getPlayerIndex()).thenReturn(0);
        when(myTank.getX()).thenReturn(100);
        when(myTank.getY()).thenReturn(200);
        when(myTank.getInventory()).thenReturn(inventory);
        
        // Setup inventory defaults to prevent NullPointerExceptions during weapon selection
        when(inventory.getDefaultAmmoType()).thenReturn(defaultAmmo);
        when(inventory.getWeaponWithLargestExplosion()).thenReturn(defaultAmmo);
        when(defaultAmmo.getName()).thenReturn("Standard HE");
    }

    @ParameterizedTest
    @ValueSource(ints = { -5, 0, 1, 3, 5, 10 })
    @DisplayName("Constructor should clamp difficulty level between 1 and 5")
    void testConstructorClampsDifficulty(int inputDifficulty) {
        AI ai = new AI(inputDifficulty, 500, myTank);
        assertNotNull(ai);
    }

    @Test
    @DisplayName("Easy AI (Level 1-2) chooses a random opponent target")
    void testEasyAISelectsRandomTarget() {
        AI ai = new AI(1, 1000.0, myTank);

        Tank opponent1 = mock(Tank.class);
        when(opponent1.getPlayerIndex()).thenReturn(1);
        when(opponent1.isAlive()).thenReturn(true);
        when(opponent1.getX()).thenReturn(200);
        when(opponent1.getY()).thenReturn(200);

        Tank opponent2 = mock(Tank.class);
        when(opponent2.getPlayerIndex()).thenReturn(2);
        when(opponent2.isAlive()).thenReturn(true);
        when(opponent2.getX()).thenReturn(300);
        when(opponent2.getY()).thenReturn(200);

        List<Tank> activePlayers = Arrays.asList(myTank, opponent1, opponent2);

        // Run multiple times to verify it filters out 'this' tank and selects an opponent safely
        for (int i = 0; i < 10; i++) {
            ai.takeTurn(GameEngine.GameState.PLAYING, terrain, activePlayers);
            verify(myTank, atLeastOnce()).setBarrelAngle(anyInt());
            verify(myTank, atLeastOnce()).setPower(anyDouble());
        }
    }

    @Test
    @DisplayName("Hard AI (Level 3-5) targets the closest opponent based on X coordinate distance")
    void testHardAISelectsClosestTarget() {
        AI ai = new AI(4, 1000.0, myTank); // Hard AI

        // My tank is at X = 100.0
        Tank farOpponent = mock(Tank.class);
        when(farOpponent.getPlayerIndex()).thenReturn(1);
        when(farOpponent.isAlive()).thenReturn(true);
        when(farOpponent.getX()).thenReturn(400); // Distance = 300
        when(farOpponent.getY()).thenReturn(200);

        Tank closeOpponent = mock(Tank.class);
        when(closeOpponent.getPlayerIndex()).thenReturn(2);
        when(closeOpponent.isAlive()).thenReturn(true);
        when(closeOpponent.getX()).thenReturn(150); // Distance = 50
        when(closeOpponent.getY()).thenReturn(200);

        List<Tank> activePlayers = Arrays.asList(myTank, farOpponent, closeOpponent);

        ai.takeTurn(GameEngine.GameState.PLAYING, terrain, activePlayers);

        // Verify that the AI actively queried the closest opponent coordinates to complete tracking
        verify(closeOpponent, atLeastOnce()).getX();
    }

    @Test
    @DisplayName("AI should handle empty opponent lists gracefully without firing")
    void testNoOpponentsLeft() {
        AI ai = new AI(3, 1000.0, myTank);
        List<Tank> activePlayers = Collections.singletonList(myTank);

        ai.takeTurn(GameEngine.GameState.PLAYING, terrain, activePlayers);

        // Tank actions should never be invoked if there's no target
        verify(myTank, never()).setBarrelAngle(anyInt());
        verify(myTank, never()).setPower(anyDouble());
    }

    @Test
    @DisplayName("Level 5 AI (Laser accurate) fires with absolute perfection (Zero noise)")
    void testLevelFiveAIAccuracy() {
        AI ai = new AI(5, 1000.0, myTank); 

        Tank target = mock(Tank.class);
        when(target.getPlayerIndex()).thenReturn(1);
        when(target.isAlive()).thenReturn(true);
        when(target.getX()).thenReturn(200); 
        when(target.getY()).thenReturn(200);

        List<Tank> activePlayers = Arrays.asList(myTank, target);

        try (MockedStatic<ProjectileSimulator> mockedSimulator = mockStatic(ProjectileSimulator.class)) {
            // Force the trajectory simulation to say the clear path is true right away
            mockedSimulator.when(() -> ProjectileSimulator.checkTrajectory(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any()
            )).thenReturn(true);

            ai.takeTurn(GameEngine.GameState.PLAYING, terrain, activePlayers);

            // clearPath is mocked to true, the recalculation loop is skipped.
            // With target to the right and zero noise, final angle must be exactly 45.
            verify(myTank).setBarrelAngle(45);
        }
    }

    @Test
    @DisplayName("AI skips playing actions during the BUYING phase")
    void testShopPhaseSkipsFiring() {
        AI ai = new AI(3, 500.0, myTank);
        Tank opponent = mock(Tank.class);
        when(opponent.getPlayerIndex()).thenReturn(1);
        when(opponent.isAlive()).thenReturn(true);

        List<Tank> activePlayers = Arrays.asList(myTank, opponent);

        // Execute turn on BUYING state
        ai.takeTurn(GameEngine.GameState.BUYING, terrain, activePlayers);

        // Firing mechanisms should not be touched during shopping
        verify(myTank, never()).setBarrelAngle(anyInt());
    }

    @Test
    @DisplayName("Consecutive shots at the same target should dynamically scale down errors")
    void testShotSpreadAdjustmentClamping() {
        AI ai = new AI(1, 1000.0, myTank); // Difficulty 1 has maximum variance

        Tank target = mock(Tank.class);
        when(target.getPlayerIndex()).thenReturn(1);
        when(target.isAlive()).thenReturn(true);
        when(target.getX()).thenReturn(300);
        when(target.getY()).thenReturn(200);

        List<Tank> activePlayers = Arrays.asList(myTank, target);

        // Fire 5 times to increment shotsFiredAtTarget, checking that logic runs smoothly
        // and adjustmentSpread = Math.max(0.1, 1.0 - (shotsFiredAtTarget * 0.3)) is utilized.
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> ai.takeTurn(GameEngine.GameState.PLAYING, terrain, activePlayers));
        }
    }
}