package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scorched.enums.PauseMenuOptions;
import scorched.weapons.HERound;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameEngineTest {

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        // Safe configuration. Asset loading errors caught internally inside GameEngine constructor.
        gameEngine = new GameEngine(800, 600);
    }

    private void setState(GameEngine.GameState state) throws Exception {
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);
        stateField.set(gameEngine, state);
    }

    private GameEngine.GameState getState() throws Exception {
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);
        return (GameEngine.GameState) stateField.get(gameEngine);
    }

    @Test
    @DisplayName("Initialization sets correct default states")
    void testInitialState() throws Exception {
        assertEquals(800, gameEngine.WIDTH);
        assertEquals(600, gameEngine.HEIGHT);
        
        assertEquals(GameEngine.GameState.MAIN_MENU, getState());
    }

    @Test
    @DisplayName("Main Menu: Menu selection toggles cleanly between Players and Hills options")
    void testMainMenuNavigation() throws Exception {
        Field menuOptField = GameEngine.class.getDeclaredField("selectedMenuOption");
        menuOptField.setAccessible(true);

        // Default should be 0 (Players)
        assertEquals(0, menuOptField.get(gameEngine));

        // Press DOWN arrow -> should change to 1 (Hills)
        KeyEvent downEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(downEvent);
        assertEquals(1, menuOptField.get(gameEngine));

        // Press UP arrow -> should return to 0 (Players)
        KeyEvent upEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(upEvent);
        assertEquals(0, menuOptField.get(gameEngine));
    }

    @Test
    @DisplayName("Main Menu: RIGHT arrow increases player count up to a maximum cap of 10")
    void testPlayerCountIncrement() throws Exception {
        Field countField = GameEngine.class.getDeclaredField("selectedPlayerCount");
        countField.setAccessible(true);
        
        // Ensure Players configuration row is highlighted (Index 0)
        Field menuOptField = GameEngine.class.getDeclaredField("selectedMenuOption");
        menuOptField.setAccessible(true);
        menuOptField.set(gameEngine, 0);

        // Default player count is 2. Press RIGHT 3 times -> should equal 5
        KeyEvent rightEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(rightEvent);
        gameEngine.keyPressed(rightEvent);
        gameEngine.keyPressed(rightEvent);

        assertEquals(5, countField.get(gameEngine));

        // Test upper boundary saturation limit (10 players max)
        for (int i = 0; i < 15; i++) {
            gameEngine.keyPressed(rightEvent);
        }
        assertEquals(10, countField.get(gameEngine), "Player count should cap strictly at 10");
    }

    @Test
    @DisplayName("Main Menu: LEFT arrow decreases player count down to a floor limit of 2")
    void testPlayerCountDecrement() throws Exception {
        Field countField = GameEngine.class.getDeclaredField("selectedPlayerCount");
        countField.setAccessible(true);
        
        // Set up base scenario above baseline floor
        countField.set(gameEngine, 4);

        // Ensure Players row is highlighted
        Field menuOptField = GameEngine.class.getDeclaredField("selectedMenuOption");
        menuOptField.setAccessible(true);
        menuOptField.set(gameEngine, 0);

        KeyEvent leftEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(leftEvent);
        assertEquals(3, countField.get(gameEngine));

        // Attempt to smash past minimum floor boundary limits
        gameEngine.keyPressed(leftEvent);
        gameEngine.keyPressed(leftEvent);
        gameEngine.keyPressed(leftEvent);

        assertEquals(2, countField.get(gameEngine), "Player count should not drop below a baseline value of 2");
    }

    @Test
    @DisplayName("Main Menu: RIGHT/LEFT arrows properly modify selected hill index layout options")
    void testHillStrengthSelectionBoundaries() throws Exception {
        Field hillIndexField = GameEngine.class.getDeclaredField("selectedHillIndex");
        hillIndexField.setAccessible(true);

        // Navigate menu selection cursor explicitly down onto Hills options row (Index 1)
        Field menuOptField = GameEngine.class.getDeclaredField("selectedMenuOption");
        menuOptField.setAccessible(true);
        menuOptField.set(gameEngine, 1);

        // Default setting starts at 0 ("Random")
        assertEquals(0, hillIndexField.get(gameEngine));

        // Cycle through options via RIGHT arrow key inputs
        KeyEvent rightEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(rightEvent); // 1: Rolling Hills
        gameEngine.keyPressed(rightEvent); // 2: Large Hills
        gameEngine.keyPressed(rightEvent); // 3: Jagged Cliffs
        assertEquals(3, hillIndexField.get(gameEngine));

        // Ensure selection hits wall clamp boundary limit at index 3
        gameEngine.keyPressed(rightEvent);
        assertEquals(3, hillIndexField.get(gameEngine), "Hill index selections must never overflow past 3");

        // Cycle backwards using LEFT arrow key inputs
        KeyEvent leftEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(leftEvent); // back down to 2
        assertEquals(2, hillIndexField.get(gameEngine));

        // Ensure selection clamps securely at baseline floor index 0
        gameEngine.keyPressed(leftEvent);
        gameEngine.keyPressed(leftEvent);
        gameEngine.keyPressed(leftEvent);
        assertEquals(0, hillIndexField.get(gameEngine), "Hill index selections must never decrement beneath 0");
    }

    @Test
    @DisplayName("Enter key switches state from MAIN_MENU to PLAYER_CONFIG")
    void testGameStartTransitionsToPlayerConfig() throws Exception {
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);

        assertEquals(GameEngine.GameState.PLAYER_CONFIG, getState(), "ENTER on MAIN_MENU should navigate to PLAYER_CONFIG");
    }

    @Test
    @DisplayName("Player Config: Confirming all players transitions to PLAYING")
    void testPlayerConfigCompletionToPlaying() throws Exception {
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        
        // Enter from MAIN_MENU into PLAYER_CONFIG (2 players by default)
        gameEngine.keyPressed(enterEvent);
        assertEquals(GameEngine.GameState.PLAYER_CONFIG, getState());

        // Confirm Player 1
        gameEngine.keyPressed(enterEvent);
        // Confirm Player 2 (final player) -> triggers startNewGame() & transitions to PLAYING
        gameEngine.keyPressed(enterEvent);

        assertEquals(GameEngine.GameState.PLAYING, getState(), "Confirming setup for all players should start the game and switch state to PLAYING");
    }

    @Test
    @DisplayName("Player Config: Text typing modifies player name and backspace deletes characters")
    void testPlayerConfigNameEditing() throws Exception {
        // Transition to PLAYER_CONFIG
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);

        Field namesField = GameEngine.class.getDeclaredField("setupPlayerNames");
        namesField.setAccessible(true);

        // Type 'A'
        KeyEvent typeA = new KeyEvent(gameEngine, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, 'A');
        gameEngine.keyTyped(typeA);

        String[] names = (String[]) namesField.get(gameEngine);
        assertEquals("Player 1A", names[0]);

        // Press Backspace
        KeyEvent backspace = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(backspace);

        names = (String[]) namesField.get(gameEngine);
        assertEquals("Player 1", names[0]);
    }

    @Test
    @DisplayName("Gameplay: Escape key pauses and unpauses the game engine fluidly")
    void testPauseStateToggle() throws Exception {
        Field pauseOptionField = GameEngine.class.getDeclaredField("selectedPauseOption");
        pauseOptionField.setAccessible(true);

        // Transition into active gameplay
        setState(GameEngine.GameState.PLAYING);
        assertEquals(GameEngine.GameState.PLAYING, getState());

        // Press Escape -> State moves to PAUSED and defaults selected pause option to SETTINGS
        KeyEvent escapeEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(escapeEvent);
        
        assertEquals(GameEngine.GameState.PAUSED, getState(), "Pressing Escape during game should switch state to PAUSED");
        assertEquals(PauseMenuOptions.SETTINGS, pauseOptionField.get(gameEngine), "Entering pause menu should reset selected option to SETTINGS");

        // Press Escape again -> Resumes execution processing smoothly back inside active gameplay state
        gameEngine.keyPressed(escapeEvent);
        assertEquals(GameEngine.GameState.PLAYING, getState(), "Pressing Escape while paused should resume back to PLAYING");
    }

    @Test
    @DisplayName("Pause Menu: UP/DOWN arrow keys correctly toggle targeted menu item options indexes")
    void testPauseMenuNavigation() throws Exception {
        Field pauseOptionField = GameEngine.class.getDeclaredField("selectedPauseOption");
        pauseOptionField.setAccessible(true);

        // Transition into PAUSED state directly
        setState(GameEngine.GameState.PAUSED);

        // Test Down selection modification -> moves focus to EXIT_BATTLE
        KeyEvent downEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(downEvent);
        assertEquals(PauseMenuOptions.EXIT_BATTLE, pauseOptionField.get(gameEngine), "DOWN arrow key should select EXIT_BATTLE");

        // Test Up selection modification -> pulls target focus index backward to SETTINGS
        KeyEvent upEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(upEvent);
        assertEquals(PauseMenuOptions.SETTINGS, pauseOptionField.get(gameEngine), "UP arrow key should navigate back to SETTINGS");
    }

    @Test
    @DisplayName("Pause Menu: Confirming 'Exit Battle' breaks from processing lifecycle loops and returns back to MAIN_MENU")
    void testPauseMenuExitAction() throws Exception {
        // Set state directly to PAUSED
        setState(GameEngine.GameState.PAUSED);

        // Highlight option 'Exit Battle'
        KeyEvent downEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(downEvent);

        // Press ENTER to register submission confirmation choice
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);
        
        assertEquals(GameEngine.GameState.MAIN_MENU, getState(), "Confirming 'Exit Battle' must return engine cleanly to MAIN_MENU state");
    }

    @Test
    @DisplayName("Pause State: Active runtime loops and entity lifecycle operations short-circuit while frozen")
    void testUpdateIsFrozenWhenPaused() throws Exception {
        // Set state to PAUSED
        setState(GameEngine.GameState.PAUSED);

        // Append mock tracking metadata items into target arrays
        Field textListField = GameEngine.class.getDeclaredField("floatingTexts");
        textListField.setAccessible(true);
        textListField.set(gameEngine, new ArrayList<FloatingText>());
        gameEngine.spawnDamageText(100, 100, 50);

        List<?> textListBefore = new ArrayList<>((List<?>) textListField.get(gameEngine));
        assertFalse(textListBefore.isEmpty());

        // Invoke private core engine update step method mechanics directly
        Method updateMethod = GameEngine.class.getDeclaredMethod("update");
        updateMethod.setAccessible(true);
        updateMethod.invoke(gameEngine);

        // Assure list values remain fully unchanged
        List<?> textListAfter = (List<?>) textListField.get(gameEngine);
        assertEquals(textListBefore.size(), textListAfter.size(), "Update loop should early-exit and not update object lifecycles when paused");
    }

    @Test
    @DisplayName("Switch Turn loops pass cleanly above incapacitated players and wrap cycle lists securely")
    void testSwitchTurnLogic() throws Exception {
        Field playersField = GameEngine.class.getDeclaredField("players");
        playersField.setAccessible(true);

        List<Tank> mockPlayers = new ArrayList<>();
        Tank p1 = mock(Tank.class);
        Tank p2 = mock(Tank.class);
        Tank p3 = mock(Tank.class);

        // Condition Profile layout details: P1 Active, P2 Inoperable/Dead, P3 Active
        when(p1.isAlive()).thenReturn(true);
        when(p2.isAlive()).thenReturn(false);
        when(p3.isAlive()).thenReturn(true);

        mockPlayers.add(p1);
        mockPlayers.add(p2);
        mockPlayers.add(p3);
        playersField.set(gameEngine, mockPlayers);

        Field activeIndexField = GameEngine.class.getDeclaredField("activePlayerIndex");
        activeIndexField.setAccessible(true);
        
        // Target index initial baseline setup (Player 1 -> Index 0)
        activeIndexField.set(gameEngine, 0);

        // Call target turn transition logic sequence method structure
        Method switchTurnMethod = GameEngine.class.getDeclaredMethod("switchTurn");
        switchTurnMethod.setAccessible(true);
        switchTurnMethod.invoke(gameEngine);

        // Confirm lifecycle pointer hops over index 1 straight onto index 2 location
        assertEquals(2, activeIndexField.get(gameEngine), "Turn should skip dead Player 2 and land directly on Player 3");
    }

    @Test
    @DisplayName("Keys configuration tracks physical structural boolean presses and release cycles perfectly")
    void testKeyTrackingArray() throws Exception {
        Field keysField = GameEngine.class.getDeclaredField("keys");
        keysField.setAccessible(true);
        boolean[] keys = (boolean[]) keysField.get(gameEngine);

        KeyEvent pressLeft = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(pressLeft);
        assertTrue(keys[KeyEvent.VK_LEFT], "Left arrow key should register as true when pressed");

        KeyEvent releaseLeft = new KeyEvent(gameEngine, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyReleased(releaseLeft);
        assertFalse(keys[KeyEvent.VK_LEFT], "Left arrow key should register as false when released");
    }

    @Test
    @DisplayName("spawnDamageText suppresses numbers less than or equal to 0 completely from lists")
    void testSpawnDamageTextFiltering() throws Exception {
        Field textListField = GameEngine.class.getDeclaredField("floatingTexts");
        textListField.setAccessible(true);
        
        textListField.set(gameEngine, new ArrayList<FloatingText>());
        
        gameEngine.spawnDamageText(100, 100, 0);
        gameEngine.spawnDamageText(100, 100, -5);
        
        List<?> textList = (List<?>) textListField.get(gameEngine);
        assertTrue(textList.isEmpty(), "Negative or zero values should not spawn visual UI numbers");
        
        gameEngine.spawnDamageText(100, 100, 25);
        assertEquals(1, textList.size(), "Positive damage values should successfully append to text tracker arrays");
    }

    @Test
    @DisplayName("executeTankFire initializes an active projectile and locks player controls")
    void testExecuteTankFire() throws Exception {
        // Arrange
        Tank mockTank = mock(Tank.class);
        when(mockTank.getX()).thenReturn(100);
        when(mockTank.getY()).thenReturn(200);
        when(mockTank.getBarrelAngle()).thenReturn(45);
        when(mockTank.getPower()).thenReturn(50.0);
        when(mockTank.getCurrentAmmoType()).thenReturn(new HERound());

        Field activeProjectileField = GameEngine.class.getDeclaredField("activeProjectile");
        activeProjectileField.setAccessible(true);
        Field lockControlsField = GameEngine.class.getDeclaredField("lockControls");
        lockControlsField.setAccessible(true);

        // Act
        gameEngine.executeTankFire(mockTank);

        // Assert
        assertNotNull(activeProjectileField.get(gameEngine), "An active projectile should be generated");
        assertTrue((boolean) lockControlsField.get(gameEngine), "Controls should lock upon firing");
    }
}