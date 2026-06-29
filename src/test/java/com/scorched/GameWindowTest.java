package com.scorched;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.awt.Frame;
import java.awt.Window;

import static org.junit.jupiter.api.Assertions.*;

public class GameWindowTest {

    private Thread gameThread;

    @BeforeEach
    void setUp() {
        // We run the main method in a background thread so it doesn't 
        // block the JUnit test execution via startGameLoop().
        gameThread = new Thread(() -> GameWindow.main(new String[]{}));
    }

    @AfterEach
    void tearDown() {
        // Clean up windows after each test to prevent memory leaks and overlapping GUIs
        for (Window window : Window.getWindows()) {
            window.dispose();
        }
        if (gameThread != null && gameThread.isAlive()) {
            gameThread.interrupt();
        }
    }

    @Test
    void testGameWindowInitialization() throws InterruptedException {
        // Start the game application
        gameThread.start();

        // Give Swing a brief moment to instantiate and render the components
        Thread.sleep(500);

        // Retrieve the active windows
        Window[] windows = Window.getWindows();
        assertTrue(windows.length > 0, "A window should have been created.");

        // Find our JFrame
        JFrame gameFrame = null;
        for (Window w : windows) {
            if (w instanceof JFrame && "Scorched".equals(((JFrame) w).getTitle())) {
                gameFrame = (JFrame) w;
                break;
            }
        }

        // --- Assertions ---
        assertNotNull(gameFrame, "Game window with title 'Scorched' was not found.");
        
        // Verify Title
        assertEquals("Scorched", gameFrame.getTitle());

        // Verify Close Operation (EXIT_ON_CLOSE corresponds to constant 3)
        assertEquals(JFrame.EXIT_ON_CLOSE, gameFrame.getDefaultCloseOperation());

        // Verify Fullscreen/Undecorated configurations
        assertTrue(gameFrame.isUndecorated(), "The window should be undecorated for fullscreen mode.");
        assertEquals(Frame.MAXIMIZED_BOTH, gameFrame.getExtendedState(), "The window should be maximized.");

        // Verify Visibility
        assertTrue(gameFrame.isVisible(), "The window should be set to visible.");

        // Verify GameEngine component was added
        boolean hasGameEngine = false;
        for (int i = 0; i < gameFrame.getContentPane().getComponentCount(); i++) {
            if (gameFrame.getContentPane().getComponent(i) instanceof GameEngine) {
                hasGameEngine = true;
                break;
            }
        }
        assertTrue(hasGameEngine, "GameEngine component should be added to the window content pane.");
    }
}