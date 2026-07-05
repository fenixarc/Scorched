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
		MAIN_MENU, PLAYING, PAUSED, GAME_OVER, BUYING
	}

	private GameState currentState = GameState.MAIN_MENU;
	private volatile boolean isGeneratingWorld = false;
	
	// Pause Menu Selection (0 = Settings, 1 = Exit Battle)
	private int selectedPauseOption;

	// Main Menu
	private BufferedImage splashImage;
	private String[] hillOptions = {"Random", "Rolling Hills", "Large Hills", "Jagged Cliffs"};
	private int selectedHillIndex;
	private int selectedMenuOption;

	// Variable to hold the current round's background color
	private Color skyColor;

	// Paired environment profiles (Sky Color, Dirt Color)
	private final EnvironmentPalette[] BATTLE_ENVIRONMENTS = {
	    new EnvironmentPalette(new Color(20, 24, 46),   new Color(115, 75, 45)),   // Deep Space / Classic Brown Earth
	    new EnvironmentPalette(new Color(40, 20, 45),   new Color(75, 50, 90)),    // Cosmic Purple / Alien Violet Crags
	    new EnvironmentPalette(new Color(15, 35, 30),   new Color(130, 145, 60)),  // Toxic Dusk / Radioactive Lime Acid
	    new EnvironmentPalette(new Color(50, 25, 20),   new Color(140, 60, 40)),   // Martian Rust / Crimson Oxide Sands
	    new EnvironmentPalette(new Color(25, 25, 25),   new Color(160, 165, 170)), // Stormy Grey / Moon Surface Basalt
	    new EnvironmentPalette(new Color(12, 16, 33),   new Color(210, 180, 140))  // Midnight Navy / Desert Dunes
	};

	// Game Classes
	private Terrain terrain;
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
	    
	    // Set Main Menu default selection
	    selectedMenuOption = 0;
	    
	    // Array of music tracks for in game
	    MusicTrack[] battleTracks = {
	        MusicTracksList.DESERT_WASTELAND,
	        MusicTracksList.NEON_CITADEL,
	        MusicTracksList.GALACTIC_DROP,
	        MusicTracksList.APEX_PREDATOR,
	        MusicTracksList.HELL_DIVER
        };
	    
	    // Select and store the music track
	    currentBattleTrack = battleTracks[rand.nextInt(battleTracks.length)];
	    SoundEngine.stopMusic();
	    SoundEngine.startMusic(currentBattleTrack);

		// Initialize players
		players = new ArrayList<>();
		Color[] playerColors = { Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.DARK_GRAY, Color.WHITE, 
				Color.PINK, Color.CYAN, Color.GRAY};

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
			
			// Temporarily make all other tanks AI
			//int aiLevel = (i > 0) ? 1 : 0;
			int aiLevel = 3;

			// Add the tank
			Tank newTank = new Tank(randomX, terrain, playerColors[i % playerColors.length], startingAngle, i, aiLevel);
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
		
		System.out.println("Starting new game: \n"
				+ "activeEnv: " + activeEnv + "\n"
				+ "selectedPlayerCount: " + selectedPlayerCount + "\n"
				+ "sectors: " + sectors.size() + "\n"
				+ "sectorWidth: " + sectorWidth + "\n"
				+ "hillStrength: " + hillStrength);
		
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
		if (currentState == GameState.PAUSED) {
			return;
		}
		
	    if (!isGeneratingWorld && currentState == GameState.PLAYING) {

	        // 1. Visual Effects (run independently of other phases)
	        for (int i = activeDebris.size() - 1; i >= 0; i--) {
	            TurretDebris d = activeDebris.get(i);
	            d.update(terrain, WIDTH, HEIGHT);
	            if (!d.isActive()) activeDebris.remove(i);
	        }
	        
	        for (int i = activeExplosions.size() - 1; i >= 0; i--) {
	            Explosion exp = activeExplosions.get(i);
	            exp.update();
	            if (!exp.isActive()) activeExplosions.remove(i);
	        }
	        
	        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
	            FloatingText ft = floatingTexts.get(i);
	            if (!ft.update()) floatingTexts.remove(i);
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
	        } 
	        else {
	        	// PHASE B: Projectile is done, wait for explosion animations to wrap up
	        	boolean explosionsRunning = !activeExplosions.isEmpty();
	            
	        	if (explosionsRunning) {
	                // Wait
	            }
	        	else {
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
		    	                if (t.isAlive()) survivorsCount++;
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
	                } 
	            	else {
		                if (keys[KeyEvent.VK_LEFT]) {
		                	SoundEngine.playBarrelRotateSound();
		                	activeTank.changeAngle(1);
		                }
		                if (keys[KeyEvent.VK_RIGHT]) {
		                	SoundEngine.playBarrelRotateSound();
		                	activeTank.changeAngle(-1);
		                }
		                if (keys[KeyEvent.VK_UP])    activeTank.changePower(0.15);
		                if (keys[KeyEvent.VK_DOWN])  activeTank.changePower(-0.15);
	                }
	            }
	        }
	    }
	}
	
	public void executeTankFire(Tank tank) {
	    double rads = Math.toRadians(tank.getBarrelAngle());
	    int startX = (int) (tank.getX() + Math.cos(rads) * 20);
	    int startY = (int) (tank.getY() - Math.sin(rads) * 20);

	    this.activeProjectile = new Projectile(startX, startY, tank.getBarrelAngle(),
	            tank.getPower(), tank.getCurrentAmmoType());
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
	    if (amount <= 0) return;
	    
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
	        case PLAYING:
	            drawGamePlay(g2d);
	            break;
	        case PAUSED:
				drawGamePlay(g2d);
				drawPauseMenu(g2d);
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

	private void drawGamePlay(Graphics2D g2d) {
		// Set random sky color
		g2d.setColor(skyColor);
		g2d.fillRect(0, 0, WIDTH, HEIGHT);

		// Draw terrain
		terrain.draw(g2d);

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
		g2d.drawString("<<< CURRENT TURN: PLAYER " + (activePlayerIndex + 1) + " >>>", WIDTH / 2 - 120, 30);

		// Draw all tank stats above their respective hulls dynamically
		for (int i = 0; i < players.size(); i++) {
			Tank t = players.get(i);
			if (t.isAlive()) {
				g2d.setColor(t.getColor());
				g2d.drawString(String.format("P%d Angle: %d°", (i + 1), t.getBarrelAngle()), t.getX() - 40,
						t.getY() - 35);
				g2d.drawString(String.format("P%d Power: %.1f", (i + 1), t.getPower()), t.getX() - 40, t.getY() - 20);
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

		// Options settings
		g2d.setFont(new Font("Arial", Font.PLAIN, 24));
		
		// Draw Options Vertically
		String[] options = {"Settings", "Exit Battle"};
		for (int i = 0; i < options.length; i++) {
			if (i == selectedPauseOption) {
				g2d.setColor(Color.CYAN);
				g2d.drawString("> " + options[i] + " <", WIDTH / 2 - 70, HEIGHT / 2 - 10 + (i * 45));
			} else {
				g2d.setColor(Color.WHITE);
				g2d.drawString(options[i], WIDTH / 2 - 45, HEIGHT / 2 - 10 + (i * 45));
			}
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
				g2d.drawString("VICTORY FOR PLAYER " + (i + 1) + "!", WIDTH / 2 - 275, HEIGHT / 2 - 20);
				winner = true;
			}
		}
		
		if(!winner) {
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
	            if (selectedMenuOption == 0 && selectedPlayerCount < 10) selectedPlayerCount++;
	            else if (selectedMenuOption == 1 && selectedHillIndex < 3) selectedHillIndex++;
	        }
	        
	        // LEFT key
	        if (keyCode == KeyEvent.VK_LEFT) {
	        	SoundEngine.playMenuSelectSound();
	        	if (selectedMenuOption == 0 && selectedPlayerCount > 2) selectedPlayerCount--;
	        	else if (selectedMenuOption == 1 && selectedHillIndex > 0) selectedHillIndex--;
	        }
			
			// ESCPAE key
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				System.exit(0);
			
			// ENTER Key
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				SoundEngine.playMenuConfirmSound();
				startNewGame();
				currentState = GameState.PLAYING;
			}
			
		}
		
		// Playing commands
		else if (currentState == GameState.PLAYING) {
			
			// ESCAPE key
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				SoundEngine.stopMusic();
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
				if ((activeProjectile == null || !activeProjectile.isActive()) && !anyTankFalling && !explosionsRunning) {
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
				SoundEngine.startMusic(currentBattleTrack);
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
					// Placeholder for Settings menu actions
					SoundEngine.playMenuConfirmSound();
					System.out.println("Settings Screen Selected");
				} else if (selectedPauseOption == 1) {
					// Exit battle back to Main Menu
					SoundEngine.playMenuConfirmSound();
					SoundEngine.stopMusic();
					currentState = GameState.MAIN_MENU;
					SoundEngine.startMusic(MusicTracksList.MENU_THEME);
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
		for (Tank tank: players) {
			if (tank.isAlive())
				activePlayers.add(tank);
		}
		return activePlayers;
	}
}