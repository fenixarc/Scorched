package com.scorched;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameEngineTest {

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        // We use a headless safe configuration or wrap initialization if assets fail.
        // Assuming asset loading issues are handled or mocked out.
        gameEngine = new GameEngine(800, 600);
    }

    @Test
    @DisplayName("Initialization sets correct default states")
    void testInitialState() throws Exception {
        assertEquals(800, gameEngine.WIDTH);
        assertEquals(600, gameEngine.HEIGHT);
        
        // Use reflection to verify private Enum State
        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);
        Object state = stateField.get(gameEngine);
        
        assertEquals("MAIN_MENU", state.toString());
    }

    @Test
    @DisplayName("Main Menu: UP arrow increases player count up to 10")
    void testPlayerCountIncrement() throws Exception {
        Field countField = GameEngine.class.getDeclaredField("selectedPlayerCount");
        countField.setAccessible(true);
        
        // Default is 2. Press up 3 times -> should be 5
        KeyEvent upEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(upEvent);
        gameEngine.keyPressed(upEvent);
        gameEngine.keyPressed(upEvent);

        assertEquals(5, countField.get(gameEngine));

        // Test upper bound limit (10)
        for (int i = 0; i < 15; i++) {
            gameEngine.keyPressed(upEvent);
        }
        assertEquals(10, countField.get(gameEngine), "Player count should cap at 10");
    }

    @Test
    @DisplayName("Main Menu: DOWN arrow decreases player count down to 2")
    void testPlayerCountDecrement() throws Exception {
        Field countField = GameEngine.class.getDeclaredField("selectedPlayerCount");
        countField.setAccessible(true);

        KeyEvent downEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
        
        // Try to go below 2
        gameEngine.keyPressed(downEvent);
        gameEngine.keyPressed(downEvent);

        assertEquals(2, countField.get(gameEngine), "Player count should not drop below 2");
    }

    @Test
    @DisplayName("Enter key switches state from MAIN_MENU to PLAYING and starts game")
    void testGameStartTransitions() throws Exception {
        KeyEvent enterEvent = new KeyEvent(gameEngine, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        gameEngine.keyPressed(enterEvent);

        Field stateField = GameEngine.class.getDeclaredField("currentState");
        stateField.setAccessible(true);
        assertEquals("PLAYING", stateField.get(gameEngine).toString());
    }

    @Test
    @DisplayName("Switch Turn skips dead tanks and cycles correctly")
    void testSwitchTurnLogic() throws Exception {
        // Set up custom player list via reflection to isolate testing turn mechanics
        Field playersField = GameEngine.class.getDeclaredField("players");
        playersField.setAccessible(true);

        List<Tank> mockPlayers = new ArrayList<>();
        Tank p1 = mock(Tank.class);
        Tank p2 = mock(Tank.class);
        Tank p3 = mock(Tank.class);

        // Scenario: P1 is alive, P2 is dead, P3 is alive
        when(p1.isAlive()).thenReturn(true);
        when(p2.isAlive()).thenReturn(false);
        when(p3.isAlive()).thenReturn(true);

        mockPlayers.add(p1);
        mockPlayers.add(p2);
        mockPlayers.add(p3);
        playersField.set(gameEngine, mockPlayers);

        Field activeIndexField = GameEngine.class.getDeclaredField("activePlayerIndex");
        activeIndexField.setAccessible(true);
        
        // Starts at index 0 (P1)
        activeIndexField.set(gameEngine, 0);

        // Invoke private method switchTurn using reflection
        java.lang.reflect.Method switchTurnMethod = GameEngine.class.getDeclaredMethod("switchTurn");
        switchTurnMethod.setAccessible(true);
        switchTurnMethod.invoke(gameEngine);

        // Index should jump over P2 (index 1) directly to P3 (index 2)
        assertEquals(2, activeIndexField.get(gameEngine), "Turn should skip dead Player 2 and land on Player 3");
    }

    @Test
    @DisplayName("Keys array correctly tracks physical presses and releases")
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
    @DisplayName("spawnDamageText suppresses numbers less than or equal to 0")
    void testSpawnDamageTextFiltering() throws Exception {
        Field textListField = GameEngine.class.getDeclaredField("floatingTexts");
        textListField.setAccessible(true);
        
        // Initialize list manually to ensure it isn't null
        textListField.set(gameEngine, new ArrayList<FloatingText>());
        
        gameEngine.spawnDamageText(100, 100, 0);
        gameEngine.spawnDamageText(100, 100, -5);
        
        List<?> textList = (List<?>) textListField.get(gameEngine);
        assertTrue(textList.isEmpty(), "Negative or zero values should not spawn visual UI numbers");
        
        gameEngine.spawnDamageText(100, 100, 25);
        assertEquals(1, textList.size(), "Positive damage values should successfully append to text tracker arrays");
    }
}