package scorched.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.awt.Frame;

import static org.junit.jupiter.api.Assertions.*;

public class GameWindowTest {

    private GameWindow gameWindow;

    @AfterEach
    void tearDown() {
        // Safely dispose of just the window created for the test
        if (gameWindow != null) {
            gameWindow.dispose();
        }
    }

    @Test
    void testGameWindowInitialization() {
        // 1. Pass tiny dimensions to a REAL GameEngine instance 
        // This avoids AWT internal NullPointerExceptions while preventing thread loops
        GameEngine dummyEngine = new GameEngine(10, 10);

        // 2. Instantiate GameWindow directly
        gameWindow = new GameWindow(dummyEngine);

        // 3. Perform your assertions
        assertEquals("Scorched", gameWindow.getTitle());
        assertEquals(JFrame.EXIT_ON_CLOSE, gameWindow.getDefaultCloseOperation());
        assertTrue(gameWindow.isUndecorated());
        assertEquals(Frame.MAXIMIZED_BOTH, gameWindow.getExtendedState());

        // Verify it was added
        assertSame(dummyEngine, gameWindow.getContentPane().getComponent(0));
    }
}