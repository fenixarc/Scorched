package scorched.game;

import javax.swing.JPanel;

import scorched.sound.MusicTrack;
import scorched.sound.MusicTracksList;
import scorched.sound.SoundEngine;
import scorched.weapons.HERound;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

public class GameEngine extends JPanel implements Runnable, KeyListener, DamageListener {

	public final int WIDTH;
	public final int HEIGHT;

	private Thread gameThread;
	private boolean isRunning = false;
	private final int FPS = 30;

	// Game States
	enum GameState {
		MAIN_MENU, PLAYER_CONFIG, PLAYING, PAUSED, GAME_OVER, BUYING, SETTINGS
	}

	private GameState currentState = GameState.MAIN_MENU;
	private volatile boolean isGeneratingWorld = false;
	
	// Player Configuration Setup Fields
	private int currentPlayerSetupIndex = 0;
	private String[] setupPlayerNames;
	private boolean[] setupPlayerIsAI;      // false = Human, true = AI
	private int[] setupPlayerDifficulty;    // 1 to 5
	private int selectedSetupOption = 0;    // 0 = Name, 1 = Type, 2 = Difficulty / Next Button
	private final String[] difficultyLabels = { "Very Easy", "Easy", "Medium", "Hard", "Very Hard" };

	// Pause Menu Selection (0 = Settings, 1 = Exit Battle)
	private int selectedPauseOption;
	
	// Settings Menu Selection (0 = Music Vol, 1 = Sound Vol, 2 = Mute Music, 3 = Mute Sound, 4 = Back)
	private int selectedSettingsOption;

	// Main Menu
	private BufferedImage splashImage;
	private String[] hillOptions = { "Random", "Rolling Hills", "Large Hills", "Jagged Cliffs" };
	private int selectedHillIndex;
	private int selectedMenuOption;

	// Variable to hold the current round's background color
	private Color skyColor;

	// Paired environment profiles (Sky Color, Dirt Color)
	private final EnvironmentPalette[] BATTLE_ENVIRONMENTS = {
			new EnvironmentPalette(new Color(20, 24, 46), new Color(115, 75, 45)), // Deep Space / Classic Brown Earth
			new EnvironmentPalette(new Color(40, 20, 45), new Color(75, 50, 90)), // Cosmic Purple / Alien Violet Crags
			new EnvironmentPalette(new Color(15, 35, 30), new Color(130, 145, 60)), // Toxic Dusk / Radioactive Lime
																					// Acid
			new EnvironmentPalette(new Color(50, 25, 20), new Color(140, 60, 40)), // Martian Rust / Crimson Oxide Sands
			new EnvironmentPalette(new Color(25, 25, 25), new Color(160, 165, 170)), // Stormy Grey / Moon Surface
																						// Basalt
			new EnvironmentPalette(new Color(12, 16, 33), new Color(210, 180, 140)) // Midnight Navy / Desert Dunes
	};

	// Game Classes
	private Terrain terrain;
	private WeatherManager weatherManager;
	private List<Tank> players;

	// Tracking Variables
	private Projectile activeProjectile;
	private List<Explosion> activeExplosions;
	private List<FloatingText> floatingTexts;
	private int selectedPlayerCount;
	private int activePlayerIndex;
	private boolean lockControls;
	private boolean isShotFired;
	private List<TurretDebris> activeDebris;
	private MusicTrack currentBattleTrack;

	// Tracks which keys are currently being held down physically
	private boolean[] keys = new boolean[256];

	/**
	 * Class Constructor.
	 */
	public GameEngine(int screenWidth, int screenHeight) {
		this.WIDTH = screenWidth;
		this.HEIGHT = screenHeight;
		this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		this.setBackground(Color.BLACK);
		this.setDoubleBuffered(true);
		this.setFocusable(true);
		this.addKeyListener(this);
		selectedPlayerCount = 2;
		selectedHillIndex = 0; // Defaults to "Random"
		selectedMenuOption = 0; // Set Main Menu default selection
		this.weatherManager = new WeatherManager(WIDTH, HEIGHT);

		// Load the splash image safely
		try {
			splashImage = ImageIO.read(new File("src/main/resources/img/Scorched Title.png"));
		} catch (IOException e) {
			System.out.println("Error: Could not find or load res/splash.png");
			e.printStackTrace();
		}

		// Play startup music
		SoundEngine.startMusic(MusicTracksList.MENU_THEME);
	}

	/**
	 * Resets everything and generates a new battlefild.
	 */
	public void startNewGame() {
		isGeneratingWorld = true;
		selectedPauseOption = 0;
		selectedSettingsOption = 0;

		// Pick a random environment bundle
		java.util.Random rand = new java.util.Random();
		EnvironmentPalette activeEnv = BATTLE_ENVIRONMENTS[rand.nextInt(BATTLE_ENVIRONMENTS.length)];

		// Set sky color
		this.skyColor = activeEnv.sky;
		this.setBackground(skyColor);

		// Initialize terrain, dirt color, and randomize hill strength
		int hillStrength;
		if (selectedHillIndex == 0)
			hillStrength = rand.nextInt(2) + 1;
		else
			hillStrength = selectedHillIndex;
		terrain = new Terrain(WIDTH, HEIGHT, activeEnv.dirt, hillStrength);

		// Pick dynamic environmental climate
		weatherManager.randomizeWeather();

		// Set Main Menu default selection
		selectedMenuOption = 0;

		// Array of music tracks for in game
		MusicTrack[] battleTracks = { MusicTracksList.DESERT_WASTELAND, MusicTracksList.NEON_CITADEL,
				MusicTracksList.GALACTIC_DROP, MusicTracksList.APEX_PREDATOR, MusicTracksList.HELL_DIVER };

		// Select and store the music track
		currentBattleTrack = battleTracks[rand.nextInt(battleTracks.length)];
		SoundEngine.stopMusic();
		SoundEngine.startMusic(currentBattleTrack);

		// Initialize players
		players = new ArrayList<>();
		Color[] playerColors = { Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.DARK_GRAY,
				Color.WHITE, Color.PINK, Color.CYAN, Color.GRAY };

		// Divide screen into sectors so tanks don't overlap, and shuffle them
		List<Integer> sectors = new ArrayList<>();
		for (int i = 0; i < selectedPlayerCount; i++) {
			sectors.add(i);
		}
		Collections.shuffle(sectors, rand);

		int sectorWidth = WIDTH / selectedPlayerCount;

		for (int i = 0; i < selectedPlayerCount; i++) {
			int assignedSector = sectors.get(i);

			// Define the horizontal boundaries for this specific player's sector
			int minX = (assignedSector * sectorWidth) + 40; // padding from left edge
			int maxX = ((assignedSector + 1) * sectorWidth) - 40; // padding from right edge

			// Pick a random X coordinate within bounds
			int randomX = minX + rand.nextInt(maxX - minX + 1);

			// Set default cannon angle
			int startingAngle = (randomX < WIDTH / 2) ? 45 : 135;

			// Setup AI and player name
			int aiLevel = setupPlayerIsAI[i] ? setupPlayerDifficulty[i] : 0; 
			String name = setupPlayerNames[i];
			
			// Add the tank
			Tank newTank = new Tank(name, randomX, terrain, playerColors[i % playerColors.length], startingAngle, i, aiLevel);
			players.add(newTank);
			newTank.setDamageListener(this);
		}

		// Reset trackers
		activeProjectile = null;
		activeExplosions = new ArrayList<>();
		floatingTexts = new ArrayList<>();
		activePlayerIndex = 0;
		lockControls = false;
		isShotFired = false;
		activeDebris = new ArrayList<>();

		System.out.println("Starting new game: \n" + "activeEnv: " + activeEnv + "\n" + "selectedPlayerCount: "
				+ selectedPlayerCount + "\n" + "sectors: " + sectors.size() + "\n" + "sectorWidth: " + sectorWidth
				+ "\n" + "hillStrength: " + hillStrength);

		isGeneratingWorld = false;
	}

	public void startGameLoop() {
		isRunning = true;
		gameThread = new Thread(this);
		gameThread.start();
	}

	@Override
	public void run() {
		double drawInterval = 1000000000 / FPS;
		double nextDrawTime = System.nanoTime() + drawInterval;

		while (isRunning) {
			update();
			repaint();

			try {
				double remainingTime = nextDrawTime - System.nanoTime();
				remainingTime = remainingTime / 1000000;

				if (remainingTime < 0) {
					remainingTime = 0;
				}

				Thread.sleep((long) remainingTime);
				nextDrawTime += drawInterval;

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void update() {
		if (currentState == GameState.PAUSED || currentState == GameState.SETTINGS) {
			return;
		}

		if (!isGeneratingWorld && currentState == GameState.PLAYING) {

			// 1. Visual Effects (run independently of other phases)
			for (int i = activeDebris.size() - 1; i >= 0; i--) {
				TurretDebris d = activeDebris.get(i);
				d.update(terrain, WIDTH, HEIGHT);
				if (!d.isActive())
					activeDebris.remove(i);
			}

			for (int i = activeExplosions.size() - 1; i >= 0; i--) {
				Explosion exp = activeExplosions.get(i);
				exp.update();
				if (!exp.isActive())
					activeExplosions.remove(i);
			}

			for (int i = floatingTexts.size() - 1; i >= 0; i--) {
				FloatingText ft = floatingTexts.get(i);
				if (!ft.update())
					floatingTexts.remove(i);
			}

			// Progress weather engine physics frame
			weatherManager.update(terrain);

			// --- ENVIRONMENT STRIKE DETECTOR ---
			if (weatherManager.hasStrikeImpacted()) {
				int lx = weatherManager.getStrikeX();
				int ly = weatherManager.getStrikeY();
				int strikeRadius = 15; // Size of impact crater
				int maxStrikeDamage = 15; // Damage cap
				
				if (weatherManager.getCurrentType() == WeatherManager.WeatherType.METEOR_SHOWER) {
			        weatherManager.consumeStrike(); // Reset the trigger flat
			    }

				// Trigger visual explosion and explode terrain instantly
	            activeExplosions.add(new Explosion(lx, ly));
	            terrain.explode(lx, ly, strikeRadius);

	            // Apply splash damage to any nearby tanks
	            for (Tank t : players) {
	                if (t.isAlive()) {
	                    double dist = Math.hypot(t.getX() - lx, t.getY() - ly);
	                    if (dist < strikeRadius) {
	                        double damageFactor = 1.0 - (dist / strikeRadius);
	                        int damage = (int) (damageFactor * maxStrikeDamage);
	                        if (damage > 0) {
	                            t.takeDamage(damage);
	                            spawnDamageText(t.getX() - 10, t.getY(), damage);
	                        }
	                    }
	                }
	            }
			}

			boolean projectileInAir = (activeProjectile != null && activeProjectile.isActive());

			// 2. Fire projectiles
			if (projectileInAir) {
				// PHASE A: Projectile is flying
				isShotFired = true;
				activeProjectile.update(terrain, players, WIDTH, HEIGHT);

				if (!activeProjectile.isActive()) {
					int ex = activeProjectile.getImpactX();
					int ey = activeProjectile.getImpactY();
					int blastRadius = activeProjectile.getExplosionRadius();

					// Only trigger explosion and damage if the shot impacts inside screen bounds
					if (ex > 0 && ex < WIDTH && ey > 0 && ey < HEIGHT) {

						// Create visual explosion and explode terrain (leaves dirt floating)
						activeExplosions.add(new Explosion(ex, ey));
						terrain.explode(ex, ey, blastRadius);

						// Calculate blast damage immediately upon impact
						for (Tank t : players) {
							if (t.isAlive()) {
								double dist = Math.hypot(t.getX() - ex, t.getY() - ey);
								if (dist < blastRadius) {
									double damageFactor = 1.0 - (dist / blastRadius);
									int damage = (int) (damageFactor * activeProjectile.getDamage());
									t.takeDamage(damage);
									spawnDamageText(t.getX() - 10, t.getY(), damage);
								}
							}
						}
					}
				}
			} else {
				// PHASE B: Projectile is done, wait for explosion animations to wrap up
				boolean explosionsRunning = !activeExplosions.isEmpty();

				if (explosionsRunning) {
					// Wait
				} else {
					// PHASE C: Projectiles and explosions are finished. Run terrain physics.
					boolean terrainFalling = terrain.update();

					if (!terrainFalling) {
						// PHASE D: Terrain has finished collapsing. Apply tank gravity.
						boolean tanksMoving = false;
						for (Tank t : players) {
							if (t.applyGravity(terrain)) {
								tanksMoving = true;
							}
						}

						// PHASE E: Tanks are stable. Turn Management and Round-End Processing
						if (lockControls && isShotFired && !tanksMoving) {
							activeProjectile = null;

							int survivorsCount = 0;
							for (Tank t : players) {
								if (t.isAlive())
									survivorsCount++;
							}

							if (survivorsCount <= 1) {
								SoundEngine.stopMusic();
								SoundEngine.startMusic(MusicTracksList.VICTORY_THEME);
								currentState = GameState.GAME_OVER;
							} else {
								switchTurn();
								lockControls = false;
								isShotFired = false;
							}
						}
					}
				}
			}

			if (!lockControls && activePlayerIndex < players.size()) {
				Tank activeTank = players.get(activePlayerIndex);
				if (activeTank.isAlive()) {
					if (activeTank.getAI() != null) {
						activeTank.getAI().takeTurn(this.currentState, terrain, getActivePlayers());
						executeTankFire(activeTank);
					} else {
						if (keys[KeyEvent.VK_LEFT]) {
							SoundEngine.playBarrelRotateSound();
							activeTank.changeAngle(1);
						}
						if (keys[KeyEvent.VK_RIGHT]) {
							SoundEngine.playBarrelRotateSound();
							activeTank.changeAngle(-1);
						}
						if (keys[KeyEvent.VK_UP])
							activeTank.changePower(0.15);
						if (keys[KeyEvent.VK_DOWN])
							activeTank.changePower(-0.15);
					}
				}
			}
		}
	}

	public void executeTankFire(Tank tank) {
		double rads = Math.toRadians(tank.getBarrelAngle());
		int startX = (int) (tank.getX() + Math.cos(rads) * 20);
		int startY = (int) (tank.getY() - Math.sin(rads) * 20);

		this.activeProjectile = new Projectile(startX, startY, tank.getBarrelAngle(), tank.getPower(),
				tank.getCurrentAmmoType());
		SoundEngine.playFireSound();
		this.lockControls = true;
	}

	/**
	 * Interface for tank fall damage.
	 */
	@Override
	public void onTankTakeDamage(int tankX, int tankY, int amount) {
		spawnDamageText(tankX - 10, tankY, amount);
	}

	/**
	 * Spawns a floating notification over a specified location.
	 */
	public void spawnDamageText(int x, int y, int amount) {
		if (amount <= 0)
			return;

		String textMsg = "-" + amount;
		// Tweak color: Light red for standard hits, bold bright red for big hits
		Color numColor = (amount > 35) ? new Color(255, 50, 50) : new Color(255, 140, 140);

		// Add text object to animate for 50 frames
		floatingTexts.add(new FloatingText(x, y, textMsg, numColor, 50));
	}

	/**
	 * Interface for tank turret explosion.
	 */
	@Override
	public void onTurretSpawned(TurretDebris debris) {
		this.activeDebris.add(debris);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;

		// Render based on what state the game is in
		switch (currentState) {
		case MAIN_MENU:
			drawMainMenu(g2d);
			break;
		case PLAYER_CONFIG:
			drawPlayerConfigMenu(g2d);
			break;
		case PLAYING:
			drawGamePlay(g2d);
			break;
		case PAUSED:
			drawGamePlay(g2d);
			drawPauseMenu(g2d);
			break;
		case SETTINGS:
			drawGamePlay(g2d);
			drawSettingsMenu(g2d);
			break;
		case GAME_OVER:
			drawEndScreen(g2d);
			break;
		case BUYING:
			// Placeholder
			break;
		}

		g2d.dispose();
	}

	private void drawCenteredString(Graphics2D g2d, String text, int y) {
		FontMetrics fm = g2d.getFontMetrics();
		int x = (WIDTH - fm.stringWidth(text)) / 2;
		g2d.drawString(text, x, y);
	}

	private void drawMainMenu(Graphics2D g2d) {
		if (splashImage != null) {
			// Draw image stretched to fit the window dimensions
			g2d.drawImage(splashImage, 0, 0, WIDTH, HEIGHT, null);
		} else {
			// Fallback text if your image fails to load
			g2d.setColor(Color.WHITE);
			drawCenteredString(g2d, "SCORCHED", HEIGHT / 2);
		}

		g2d.setFont(new Font("Arial", Font.BOLD, 18));
		g2d.setColor(Color.BLUE);
		drawCenteredString(g2d, "UP / DOWN ARROWS CHANGE SELECTION", HEIGHT / 2 - 80);
		drawCenteredString(g2d, "RIGHT / LEFT ARROWS CHANGE SETTING", HEIGHT / 2 - 60);

		// ==========================================
		// BOX 1: PLAYERS SELECTION
		// ==========================================
		g2d.setColor(new Color(25, 30, 55));
		g2d.fillRect(WIDTH / 2 - 150, HEIGHT / 2 - 15, 300, 60);
		// Highlight if selected
		if (selectedMenuOption == 0)
			g2d.setColor(Color.YELLOW);
		else
			g2d.setColor(Color.CYAN);
		g2d.drawRect(WIDTH / 2 - 150, HEIGHT / 2 - 15, 300, 60);

		g2d.setFont(new Font("Arial", Font.BOLD, 24));
		g2d.setColor(Color.WHITE);
		g2d.drawString("PLAYERS: " + selectedPlayerCount, WIDTH / 2 - 65, HEIGHT / 2 + 23);

		// ==========================================
		// BOX 2: HILL STRENGTH SELECTION
		// ==========================================
		// Shifted down by 80 pixels (60px box height + 20px gap)
		int box2Y = HEIGHT / 2 - 15 + 80;

		g2d.setColor(new Color(25, 30, 55));
		g2d.fillRect(WIDTH / 2 - 150, box2Y, 300, 60);
		// Highlight if selected
		if (selectedMenuOption == 1)
			g2d.setColor(Color.YELLOW);
		else
			g2d.setColor(Color.CYAN);
		g2d.drawRect(WIDTH / 2 - 150, box2Y, 300, 60);

		// Draw the text dynamically from array
		g2d.setFont(new Font("Arial", Font.BOLD, 20));
		g2d.setColor(Color.WHITE);
		String hillText = "HILLS: " + hillOptions[selectedHillIndex].toUpperCase();

		// Simple dynamic centering calculation for the text inside the box
		int textWidth = g2d.getFontMetrics().stringWidth(hillText);
		g2d.drawString(hillText, WIDTH / 2 - (textWidth / 2), box2Y + 37);

		// ==========================================
		// FOOTER
		// ==========================================
		g2d.setFont(new Font("Arial", Font.PLAIN, 18));
		g2d.setColor(Color.YELLOW);
		drawCenteredString(g2d, "PRESS ENTER TO START GAME", HEIGHT - 50);
	}
	
	private void drawPlayerConfigMenu(Graphics2D g2d) {
		if (splashImage != null) {
			g2d.drawImage(splashImage, 0, 0, WIDTH, HEIGHT, null);
		} else {
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, WIDTH, HEIGHT);
		}

		g2d.setFont(new Font("Arial", Font.BOLD, 28));
		g2d.setColor(Color.YELLOW);
		drawCenteredString(g2d, "CONFIGURE PLAYER " + (currentPlayerSetupIndex + 1) + " / " + selectedPlayerCount, HEIGHT / 2 - 140);

		g2d.setFont(new Font("Arial", Font.BOLD, 16));
		g2d.setColor(Color.BLUE);
		drawCenteredString(g2d, "UP / DOWN TO NAVIGATE OPTIONS", HEIGHT / 2 - 100);
		drawCenteredString(g2d, "TYPE TO EDIT NAME | LEFT / RIGHT TO CHANGE SETTINGS", HEIGHT / 2 - 80);

		boolean isAI = setupPlayerIsAI[currentPlayerSetupIndex];

		// ==========================================
		// BOX 1: EDITABLE NAME BOX
		// ==========================================
		int box1Y = HEIGHT / 2 - 50;
		g2d.setColor(new Color(25, 30, 55));
		g2d.fillRect(WIDTH / 2 - 175, box1Y, 350, 50);
		g2d.setColor(selectedSetupOption == 0 ? Color.YELLOW : Color.CYAN);
		g2d.drawRect(WIDTH / 2 - 175, box1Y, 350, 50);

		g2d.setFont(new Font("Arial", Font.BOLD, 20));
		g2d.setColor(Color.WHITE);
		String nameText = "NAME: " + setupPlayerNames[currentPlayerSetupIndex];
		// Add a blinking cursor effect if selected
		if (selectedSetupOption == 0 && (System.currentTimeMillis() / 500) % 2 == 0) {
			nameText += "|";
		}
		g2d.drawString(nameText, WIDTH / 2 - 150, box1Y + 32);

		// ==========================================
		// BOX 2: CONTROLLER TYPE (HUMAN / AI)
		// ==========================================
		int box2Y = box1Y + 65;
		g2d.setColor(new Color(25, 30, 55));
		g2d.fillRect(WIDTH / 2 - 175, box2Y, 350, 50);
		g2d.setColor(selectedSetupOption == 1 ? Color.YELLOW : Color.CYAN);
		g2d.drawRect(WIDTH / 2 - 175, box2Y, 350, 50);

		String typeText = "CONTROL: " + (isAI ? "AI" : "HUMAN");
		g2d.drawString(typeText, WIDTH / 2 - 150, box2Y + 32);

		// ==========================================
		// BOX 3: DIFFICULTY (ONLY VISIBLE IF AI)
		// ==========================================
		int nextButtonY = box2Y + 65;
		if (isAI) {
			g2d.setColor(new Color(25, 30, 55));
			g2d.fillRect(WIDTH / 2 - 175, nextButtonY, 350, 50);
			g2d.setColor(selectedSetupOption == 2 ? Color.YELLOW : Color.CYAN);
			g2d.drawRect(WIDTH / 2 - 175, nextButtonY, 350, 50);

			int diffValue = setupPlayerDifficulty[currentPlayerSetupIndex];
			String diffText = "DIFFICULTY: " + difficultyLabels[diffValue - 1].toUpperCase();
			g2d.drawString(diffText, WIDTH / 2 - 150, nextButtonY + 32);
			
			nextButtonY += 65;
		}

		// ==========================================
		// BOX 4: CONFIRMATION / PROGRESSION BUTTON
		// ==========================================
		g2d.setColor(new Color(25, 30, 55));
		g2d.fillRect(WIDTH / 2 - 175, nextButtonY, 350, 50);
		
		int confirmOptionIndex = isAI ? 3 : 2;
		g2d.setColor(selectedSetupOption == confirmOptionIndex ? Color.YELLOW : Color.CYAN);
		g2d.drawRect(WIDTH / 2 - 175, nextButtonY, 350, 50);

		String buttonLabel = (currentPlayerSetupIndex == selectedPlayerCount - 1) ? "START MATCH" : "NEXT PLAYER";
		g2d.setColor(Color.WHITE);
		int btnTextWidth = g2d.getFontMetrics().stringWidth(buttonLabel);
		g2d.drawString(buttonLabel, WIDTH / 2 - (btnTextWidth / 2), nextButtonY + 32);
	}

	private void drawGamePlay(Graphics2D g2d) {
		// Set random sky color
		g2d.setColor(skyColor);
		g2d.fillRect(0, 0, WIDTH, HEIGHT);

		// Draw terrain
		terrain.draw(g2d);

		// Draw weather layers
		weatherManager.draw(g2d);

		// Draw all tanks
		for (Tank t : players) {
			t.draw(g2d);
		}

		// Draw all exploding turrets
		for (TurretDebris d : activeDebris) {
			d.draw(g2d);
		}

		// Draw projectiles if active
		if (activeProjectile != null && activeProjectile.isActive()) {
			activeProjectile.draw(g2d);
		}

		// Draw explosions
		for (Explosion exp : activeExplosions) {
			exp.draw(g2d);
		}

		// Draw damage numbers
		for (FloatingText ft : floatingTexts) {
			ft.draw(g2d);
		}

		// Draw UI Text
		g2d.setColor(Color.WHITE);
		g2d.drawString("Controls: LEFT/RIGHT to Aim | UP/DOWN for Power | SPACEBAR to fire | ESC to Exit", 20, 30);

		// Set player turn display
		Tank activeTank = players.get(activePlayerIndex);
		g2d.setColor(activeTank.getColor());
		String turnText = "<<< CURRENT TURN: " + activeTank.getName().toUpperCase() + " >>>";
		FontMetrics fm = g2d.getFontMetrics();
		g2d.drawString(turnText, (WIDTH - fm.stringWidth(turnText)) / 2, 30);

		// Draw all tank stats above their respective hulls dynamically
		for (int i = 0; i < players.size(); i++) {
		    Tank t = players.get(i);
		    if (t.isAlive()) {
		        g2d.setColor(t.getColor());
		        
		        // Dynamically measure name string size for centralized anchoring above the asset
		        int nameWidth = g2d.getFontMetrics().stringWidth(t.getName());
		        g2d.drawString(t.getName(), t.getX() - (nameWidth / 2), t.getY() - 50);
		        
		        g2d.drawString(String.format("Angle: %d°", t.getBarrelAngle()), t.getX() - 40, t.getY() - 35);
		        g2d.drawString(String.format("Power: %.1f", t.getPower()), t.getX() - 40, t.getY() - 20);
		    }
		}
	}

	private void drawPauseMenu(Graphics2D g2d) {
		// Add a semi-transparent overlay to dim the screen
		g2d.setColor(new Color(0, 0, 0, 160));
		g2d.fillRect(0, 0, WIDTH, HEIGHT);

		// Render Title
		g2d.setFont(new Font("Arial", Font.BOLD, 36));
		g2d.setColor(Color.YELLOW);
		g2d.drawString("GAME PAUSED", WIDTH / 2 - 120, HEIGHT / 2 - 80);

		// Style configuration matching the Main Menu
				String[] options = { "SETTINGS", "EXIT BATTLE" };
				g2d.setFont(new Font("Arial", Font.BOLD, 24));

				// Draw Options Vertically using Main Menu stylized boxes
				for (int i = 0; i < options.length; i++) {
					// Calculate vertical position to match main menu box pacing (60px height + 20px gap)
					int boxY = HEIGHT / 2 - 15 + (i * 80);

					// 1. Draw Background Box
					g2d.setColor(new Color(25, 30, 55));
					g2d.fillRect(WIDTH / 2 - 150, boxY, 300, 60);

					// 2. Highlight border based on selection
					if (i == selectedPauseOption) {
						g2d.setColor(Color.YELLOW);
					} else {
						g2d.setColor(Color.CYAN);
					}
					g2d.drawRect(WIDTH / 2 - 150, boxY, 300, 60);

					// 3. Draw perfectly centered white text inside the box
					g2d.setColor(Color.WHITE);
					FontMetrics fm = g2d.getFontMetrics();
					int textX = WIDTH / 2 - (fm.stringWidth(options[i]) / 2);
					int textY = boxY + 37; // Centers text baseline vertically inside the 60px box
					g2d.drawString(options[i], textX, textY);
				}
	}
	
	private void drawSettingsMenu(Graphics2D g2d) {
		g2d.setColor(new Color(0, 0, 0, 160));
		g2d.fillRect(0, 0, WIDTH, HEIGHT);

		// Render Settings Title
		g2d.setFont(new Font("Arial", Font.BOLD, 36));
		g2d.setColor(Color.YELLOW);
		g2d.drawString("SETTINGS", WIDTH / 2 - 95, HEIGHT / 2 - 190);

		g2d.setFont(new Font("Arial", Font.BOLD, 20));
		
		// Map our internal properties out to text declarations
		String[] options = {
			"MUSIC VOLUME: " + SoundEngine.musicVolume,
			"SOUND VOLUME: " + SoundEngine.soundVolume,
			"MUTE MUSIC: [ " + (SoundEngine.muteMusic ? "X" : " ") + " ]",
			"MUTE SOUND: [ " + (SoundEngine.muteSound ? "X" : " ") + " ]",
			"BACK"
		};

		for (int i = 0; i < options.length; i++) {
			// Offset options safely starting higher up to anchor 5 vertical options cleanly
			int boxY = HEIGHT / 2 - 130 + (i * 70);

			g2d.setColor(new Color(25, 30, 55));
			g2d.fillRect(WIDTH / 2 - 165, boxY, 330, 50);

			if (i == selectedSettingsOption) {
				g2d.setColor(Color.YELLOW);
			} else {
				g2d.setColor(Color.CYAN);
			}
			g2d.drawRect(WIDTH / 2 - 165, boxY, 330, 50);

			g2d.setColor(Color.WHITE);
			FontMetrics fm = g2d.getFontMetrics();
			int textX = WIDTH / 2 - (fm.stringWidth(options[i]) / 2);
			int textY = boxY + 31;
			g2d.drawString(options[i], textX, textY);
		}
	}

	private void drawEndScreen(Graphics2D g2d) {

		// Keep the final battlefield image
		g2d.setColor(skyColor);
		g2d.fillRect(0, 0, WIDTH, HEIGHT);
		terrain.draw(g2d);

		// Draw all tanks
		for (Tank t : players) {
			t.draw(g2d);
		}

		// Draw all exploding turrets
		for (TurretDebris d : activeDebris) {
			d.draw(g2d);
		}

		// Draw explosions
		for (Explosion exp : activeExplosions) {
			exp.draw(g2d);
		}

		// Draw damage numbers
		for (FloatingText ft : floatingTexts) {
			ft.draw(g2d);
		}

		// Add a smoky dark veil to isolate the screen text
		g2d.setColor(new Color(0, 0, 0, 195));
		g2d.fillRect(0, 0, WIDTH, HEIGHT);

		// Draw Winner Proclamation
		g2d.setColor(Color.YELLOW);
		g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 42));

		// Find Winner (if any)
		boolean winner = false;
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).isAlive()) {
				g2d.setColor(players.get(i).getColor());
				g2d.drawString("VICTORY FOR " + players.get(i).getName() + "!", WIDTH / 2 - 275, HEIGHT / 2 - 20);
				winner = true;
			}
		}

		if (!winner) {
			g2d.setColor(Color.WHITE);
			g2d.drawString("DRAW!", WIDTH / 2 - 275, HEIGHT / 2 - 20);
		}

		g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 20));
		g2d.setColor(Color.WHITE);
		g2d.drawString("Press ESC to Exit the battle", WIDTH / 2 - 140, HEIGHT / 2 + 40);
	}

	/**
	 * Handles keyboard presses in different game modes.
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();

		// Safety check to avoid ArrayOutOfBoundsException if an exotic key is pressed
		if (keyCode >= 0 && keyCode < keys.length) {
			keys[keyCode] = true;
		}

		// Main Menu commands
		if (currentState == GameState.MAIN_MENU) {

			// UP key
			if (keyCode == KeyEvent.VK_UP) {
				SoundEngine.playMenuSelectSound();
				selectedMenuOption = 0;
			}

			// DOWN key
			if (keyCode == KeyEvent.VK_DOWN) {
				SoundEngine.playMenuSelectSound();
				selectedMenuOption = 1;
			}

			// RIGHT key
			if (keyCode == KeyEvent.VK_RIGHT) {
				SoundEngine.playMenuSelectSound();
				if (selectedMenuOption == 0 && selectedPlayerCount < 10)
					selectedPlayerCount++;
				else if (selectedMenuOption == 1 && selectedHillIndex < 3)
					selectedHillIndex++;
			}

			// LEFT key
			if (keyCode == KeyEvent.VK_LEFT) {
				SoundEngine.playMenuSelectSound();
				if (selectedMenuOption == 0 && selectedPlayerCount > 2)
					selectedPlayerCount--;
				else if (selectedMenuOption == 1 && selectedHillIndex > 0)
					selectedHillIndex--;
			}

			// ESCPAE key
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				System.exit(0);

			// ENTER Key
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				SoundEngine.playMenuConfirmSound();
				
				// Initialize setup configurations based on the selected player count
				currentPlayerSetupIndex = 0;
				selectedSetupOption = 0;
				setupPlayerNames = new String[selectedPlayerCount];
				setupPlayerIsAI = new boolean[selectedPlayerCount];
				setupPlayerDifficulty = new int[selectedPlayerCount];
				
				for (int i = 0; i < selectedPlayerCount; i++) {
					setupPlayerNames[i] = "Player " + (i + 1);
					setupPlayerIsAI[i] = (i > 0); // Default first player to Human, others to AI
					setupPlayerDifficulty[i] = 3;  // Default to Medium (3)
				}
				
				currentState = GameState.PLAYER_CONFIG;
			}

		}
		
		// Player Config Commands
		else if (currentState == GameState.PLAYER_CONFIG) {
			boolean isAI = setupPlayerIsAI[currentPlayerSetupIndex];
			int totalOptions = isAI ? 4 : 3;

			// UP Key
			if (keyCode == KeyEvent.VK_UP) {
				SoundEngine.playMenuSelectSound();
				selectedSetupOption = (selectedSetupOption - 1 + totalOptions) % totalOptions;
			}
			
			// DOWN Key
			if (keyCode == KeyEvent.VK_DOWN) {
				SoundEngine.playMenuSelectSound();
				selectedSetupOption = (selectedSetupOption + 1) % totalOptions;
			}

			// LEFT / RIGHT Key
			if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
				SoundEngine.playMenuSelectSound();
				if (selectedSetupOption == 1) {
					// Toggle AI / Human
					setupPlayerIsAI[currentPlayerSetupIndex] = !setupPlayerIsAI[currentPlayerSetupIndex];
					// Reset selection validation context bounds if toggling fields away
					if (!setupPlayerIsAI[currentPlayerSetupIndex] && selectedSetupOption == 3) {
						selectedSetupOption = 2;
					}
				} else if (selectedSetupOption == 2 && isAI) {
					// Adjust Difficulty
					int currentDiff = setupPlayerDifficulty[currentPlayerSetupIndex];
					if (keyCode == KeyEvent.VK_RIGHT && currentDiff < 5) setupPlayerDifficulty[currentPlayerSetupIndex]++;
					if (keyCode == KeyEvent.VK_LEFT && currentDiff > 1) setupPlayerDifficulty[currentPlayerSetupIndex]--;
				}
			}

			// BACKSPACE Key
			if (keyCode == KeyEvent.VK_BACK_SPACE && selectedSetupOption == 0) {
				String currentName = setupPlayerNames[currentPlayerSetupIndex];
				if (currentName.length() > 0) {
					setupPlayerNames[currentPlayerSetupIndex] = currentName.substring(0, currentName.length() - 1);
				}
			}

			// ESCAPE Key
			if (keyCode == KeyEvent.VK_ESCAPE) {
				SoundEngine.playMenuConfirmSound();
				currentState = GameState.MAIN_MENU;
			}

			// ENTER Key
			if (keyCode == KeyEvent.VK_ENTER) {
				SoundEngine.playMenuConfirmSound();
				if (currentPlayerSetupIndex < selectedPlayerCount - 1) {
					currentPlayerSetupIndex++;
					selectedSetupOption = 0; // Reset focus to top for next player
				} else {
					startNewGame();
					currentState = GameState.PLAYING;
				}
			}
		}

		// Playing commands
		else if (currentState == GameState.PLAYING) {

			// ESCAPE key
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				//SoundEngine.stopMusic();
				SoundEngine.playPauseSound();
				selectedPauseOption = 0;
				currentState = GameState.PAUSED;
			}

			// SPACE key
			if (e.getKeyCode() == KeyEvent.VK_SPACE && !lockControls) {
				boolean anyTankFalling = false;
				for (Tank t : players) {
					if (t.applyGravity(terrain))
						anyTankFalling = true;
				}

				boolean explosionsRunning = !activeExplosions.isEmpty();

				// Only fire if all other actions are complete
				if ((activeProjectile == null || !activeProjectile.isActive()) && !anyTankFalling
						&& !explosionsRunning) {
					Tank currentTank = players.get(activePlayerIndex);

					double rads = Math.toRadians(currentTank.getBarrelAngle());
					int startX = (int) (currentTank.getX() + Math.cos(rads) * 20);
					int startY = (int) (currentTank.getY() - Math.sin(rads) * 20);

					activeProjectile = new Projectile(startX, startY, currentTank.getBarrelAngle(),
							currentTank.getPower(), new HERound());
					SoundEngine.playFireSound();
					lockControls = true;
				}
			}
		}

		// Pause Menu Commands
		else if (currentState == GameState.PAUSED) {

			// ESCAPE key
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				//SoundEngine.startMusic(currentBattleTrack);
				SoundEngine.playUnpauseSound();
				currentState = GameState.PLAYING;
			}

			// UP Key
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				SoundEngine.playMenuSelectSound();
				selectedPauseOption = 0;
			}

			// DOWN Key
			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				SoundEngine.playMenuSelectSound();
				selectedPauseOption = 1;
			}

			// ENTER Key
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (selectedPauseOption == 0) {
					SoundEngine.playMenuConfirmSound();
					selectedSettingsOption = 0;
					currentState = GameState.SETTINGS;
				} else if (selectedPauseOption == 1) {
					SoundEngine.playMenuConfirmSound();
					SoundEngine.stopMusic();
					currentState = GameState.MAIN_MENU;
					SoundEngine.startMusic(MusicTracksList.MENU_THEME);
				}
			}
		}
		
		// Settings Menu Commands
		else if (currentState == GameState.SETTINGS) {
			
			// ESCAPE Key
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				SoundEngine.playMenuConfirmSound();
				currentState = GameState.PAUSED;
			}

			// UP Key
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				SoundEngine.playMenuSelectSound();
				selectedSettingsOption--;
				if (selectedSettingsOption < 0) {
					selectedSettingsOption = 4; // Wrap around to back
				}
			}

			// DOWN Key
			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				SoundEngine.playMenuSelectSound();
				selectedSettingsOption++;
				if (selectedSettingsOption > 4) {
					selectedSettingsOption = 0; // Wrap around to top
				}
			}

			// RIGHT Key
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				SoundEngine.playMenuSelectSound();
				if (selectedSettingsOption == 0 && SoundEngine.musicVolume < 10) {
					SoundEngine.musicVolume++;
				} else if (selectedSettingsOption == 1 && SoundEngine.soundVolume < 10) {
					SoundEngine.soundVolume++;
				}
			}

			// LEFT Key
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				SoundEngine.playMenuSelectSound();
				if (selectedSettingsOption == 0 && SoundEngine.musicVolume > 1) {
					SoundEngine.musicVolume--;
				} else if (selectedSettingsOption == 1 && SoundEngine.soundVolume > 1) {
					SoundEngine.soundVolume--;
				}
			}

			// ENTER Key
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				SoundEngine.playMenuConfirmSound();
				if (selectedSettingsOption == 2) {
					SoundEngine.muteMusic = !SoundEngine.muteMusic;
				} else if (selectedSettingsOption == 3) {
					SoundEngine.muteSound = !SoundEngine.muteSound;
				} else if (selectedSettingsOption == 4) {
					currentState = GameState.PAUSED;
				}
			}
		}

		// Game over commands
		else if (currentState == GameState.GAME_OVER) {

			// ESCAPE key
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				SoundEngine.stopMusic();
				currentState = GameState.MAIN_MENU;
				SoundEngine.startMusic(MusicTracksList.MENU_THEME);
			}
		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();

		// When the key is lifted, set its state to false
		if (keyCode >= 0 && keyCode < keys.length) {
			keys[keyCode] = false;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (currentState == GameState.PLAYER_CONFIG && selectedSetupOption == 0) {
			char c = e.getKeyChar();
			// Accept only printable characters up to a reasonable length limit
			if (c != KeyEvent.CHAR_UNDEFINED && c != '\n' && c != '\b' && setupPlayerNames[currentPlayerSetupIndex].length() < 15) {
				setupPlayerNames[currentPlayerSetupIndex] += c;
			}
		}
	}

	/**
	 * Gives control to the next alive tank.
	 */
	private void switchTurn() {
		do {
			// Advance pointer line by 1, wrapping around array borders
			activePlayerIndex = (activePlayerIndex + 1) % players.size();
		} while (!players.get(activePlayerIndex).isAlive()); // Keep moving if the tank is dead
	}

	/**
	 * Passes back a list of Tanks that are still alive.
	 */
	private List<Tank> getActivePlayers() {
		List<Tank> activePlayers = new ArrayList<Tank>();
		for (Tank tank : players) {
			if (tank.isAlive())
				activePlayers.add(tank);
		}
		return activePlayers;
	}
}