package scorched.game;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Toolkit;

public class GameWindow extends JFrame {

    // Constructor handles configuration and dependency injection
    public GameWindow(GameEngine gameEngine) {
        super("Scorched");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Fullscreen borderless
        this.setUndecorated(true);

        // Setup gameEngine component
        this.add(gameEngine);

        // Fit the window
        this.pack();

        // Lock window to top-left corner and maximize across screen
        this.setLocationRelativeTo(null);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public static void main(String[] args) {
        // Gather environment data
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        // Create dependencies
        GameEngine gameEngine = new GameEngine(screenWidth, screenHeight);

        // Construct and display the window
        GameWindow window = new GameWindow(gameEngine);
        window.setVisible(true);

        System.out.println("Game window created");

        // Start execution
        gameEngine.startGameLoop();
    }
}