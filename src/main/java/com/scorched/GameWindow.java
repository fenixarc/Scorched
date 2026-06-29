package com.scorched;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Toolkit;

public class GameWindow {

	public static void main(String[] args) {
		JFrame window = new JFrame();

		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setTitle("Scorched");

		// Fullscreen borderless
		window.setUndecorated(true);

		// Get screen dimensions
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenWidth = (int) screenSize.getWidth();
		int screenHeight = (int) screenSize.getHeight();

		// Setup gameEngine
		GameEngine gameEngine = new GameEngine(screenWidth, screenHeight);
		window.add(gameEngine);

		// Fit the window
		window.pack();

		// Lock window to top-left corner and maximize across screen
		window.setLocationRelativeTo(null);
		window.setExtendedState(JFrame.MAXIMIZED_BOTH);

		window.setVisible(true);

		System.out.println("Game window created");

		gameEngine.startGameLoop();
	}
}