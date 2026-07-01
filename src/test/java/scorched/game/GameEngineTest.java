package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @Test
    @DisplayName("Initialization sets correct default states")
    void testInitialState() throws Exception {
        assertEquals(800, gameEngine.WIDTH);
        assertEquals(600, gameEngine.HEIGHT);
        
        // Verify initial private Enum State
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);
        Object state = stateField.get(gameEngine);
        
        assertEquals("MAIN_MENU", state.toString());
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
    @DisplayName("Enter key switches state from MAIN_MENU to PLAYING and starts game loop execution")
    void testGameStartTransitions() throws Exception {
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);

        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);
        assertEquals("PLAYING", stateField.get(gameEngine).toString());
    }

    @Test
    @DisplayName("Gameplay: Escape key pauses and unpauses the game engine fluidly")
    void testPauseStateToggle() throws Exception {
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);

        Field pauseOptionField = GameEngine.class.getDeclaredField("selectedPauseOption");
        pauseOptionField.setAccessible(true);

        // Force transition out of Main Menu directly into active state gameplay
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);
        assertEquals("PLAYING", stateField.get(gameEngine).toString());

        // Press Escape -> State moves to PAUSED and focuses selection at index 0 (Settings)
        KeyEvent escapeEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(escapeEvent);
        
        assertEquals("PAUSED", stateField.get(gameEngine).toString(), "Pressing Escape during game should switch state to PAUSED");
        assertEquals(0, pauseOptionField.get(gameEngine), "Entering pause menu should reset selected index to 0 (Settings)");

        // Press Escape again -> Resumes execution processing smoothly back inside active gameplay state
        gameEngine.keyPressed(escapeEvent);
        assertEquals("PLAYING", stateField.get(gameEngine).toString(), "Pressing Escape while paused should resume back to PLAYING");
    }

    @Test
    @DisplayName("Pause Menu: UP/DOWN arrow keys correctly toggle targeted menu item options indexes")
    void testPauseMenuNavigation() throws Exception {
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);
        
        Field pauseOptionField = GameEngine.class.getDeclaredField("selectedPauseOption");
        pauseOptionField.setAccessible(true);

        // Route state directly into active pause menu mode
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);
        KeyEvent escapeEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(escapeEvent);

        // Test Down selection modification -> increments list cursor forward to 1 (Exit Battle)
        KeyEvent downEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(downEvent);
        assertEquals(1, pauseOptionField.get(gameEngine), "DOWN arrow key should select index 1 (Exit Battle)");

        // Test Up selection modification -> pulls target focus index backward to 0 (Settings)
        KeyEvent upEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(upEvent);
        assertEquals(0, pauseOptionField.get(gameEngine), "UP arrow key should navigate back to index 0 (Settings)");
    }

    @Test
    @DisplayName("Pause Menu: Confirming 'Exit Battle' breaks from processing lifecycle loops and returns back to MAIN_MENU")
    void testPauseMenuExitAction() throws Exception {
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);

        // Move execution states downstream into active pause mode
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);
        KeyEvent escapeEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(escapeEvent);

        // Highlight option index 1 ('Exit Battle')
        KeyEvent downEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(downEvent);

        // Press ENTER to register submission confirmation choice
        gameEngine.keyPressed(enterEvent);
        assertEquals("MAIN_MENU", stateField.get(gameEngine).toString(), "Confirming 'Exit Battle' must return engine cleanly to MAIN_MENU state");
    }

    @Test
    @DisplayName("Pause State: Active runtime loops and entity lifecycle operations short-circuit while frozen")
    void testUpdateIsFrozenWhenPaused() throws Exception {
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);

        // Route state loops down into active PAUSED mode processing structures
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);
        KeyEvent escapeEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(escapeEvent);

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

        // Assure lists values remain fully unchanged
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
}