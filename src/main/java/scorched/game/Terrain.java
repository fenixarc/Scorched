package scorched.game;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

public class Terrain {

	private boolean[][] solidGrid;
	private int screenWidth;
	private int screenHeight;
	private Color dirtColor;

	private static final int GRAVITY = 4;

	public Terrain(int screenWidth, int screenHeight, Color dirtColor, int hillStrength) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.solidGrid = new boolean[screenWidth][screenHeight];
		this.dirtColor = dirtColor;

		generateTerrain(hillStrength);
	}

	/**
	 * Generates a randomized set of rolling hills and fills the 2D grid.
	 * @param hillStrength 1 = standard rolling hills, 2 = large hills/valleys, 3 = extreme jagged cliffs
	 */
	private void generateTerrain(int hillStrength) {
		Random rand = new Random();

		// Randomize the baseline ground level slightly (between 55% and 75% of screen height)
		double randomBaselinePercent = 0.55 + (rand.nextDouble() * 0.20);
		int baselineY = (int) (screenHeight * randomBaselinePercent);

		// Generate random offsets so waves shift horizontally left or right uniquely
		double offset1 = rand.nextDouble() * 10000;
		double offset2 = rand.nextDouble() * 10000;
		double offset3 = rand.nextDouble() * 10000;

		// Default base parameters (hillStrength == 1)
		double largeHillsVariance = screenHeight * (0.08 + rand.nextDouble() * 0.12); // Large hills height variance
		double mediumHillsVariance = screenHeight * (0.02 + rand.nextDouble() * 0.04); // Medium hills height variance
		double jaggednessIntensity = 3 + rand.nextInt(6); // Jaggedness intensity
		
		// Frequency multipliers to control horizontal stretching/crunching
		double freq1 = 0.002;
		double freq2 = 0.01;
		double freq3 = 0.03;

		// Scale the properties based on hillStrength
		if (hillStrength == 2) {
			// Tier 2: Large Hills and Valleys (Double the amplitude, stretch out horizontally)
			largeHillsVariance *= 2.0;
			mediumHillsVariance *= 2.0;
			freq1 *= 0.7; // Stretches the valleys wider
		} else if (hillStrength == 3) {
			// Tier 3: Extreme Jagged Cliffs (Massive amplitude + intense, frequent spikes)
			largeHillsVariance *= 3.0;
			mediumHillsVariance *= 2.5;
			jaggednessIntensity *= 8.0; // Heavily amplifies the jagged wave texture
			
			freq1 *= 1.5; // Crunches waves together for steeper slopes
			freq3 *= 1.2; // Makes cliffs look sharper and more chaotic
		}

		// Calculate the height array column-by-column
		for (int x = 0; x < screenWidth; x++) {
			// Large rolling peaks
			double wave1 = Math.sin((x + offset1) * freq1) * largeHillsVariance;
			// Secondary hills
			double wave2 = Math.sin((x + offset2) * freq2) * mediumHillsVariance;
			// Jagged terrain texture
			double wave3 = Math.cos((x + offset3) * freq3) * jaggednessIntensity;

			// Combine calculations into the column height index
			int surfaceY = (int) (baselineY - (wave1 + wave2 + wave3));

			// --- Bounds enforcement with 40px buffer ---
			/*
			 * // Cannot go below the bottom of the screen (-40px buffer) if (surfaceY >
			 * screenHeight - 40) { surfaceY = screenHeight - 40; } // Cannot go above the
			 * top of the screen (+60px buffer) if (surfaceY < 60) { surfaceY = 60; }
			 */
			
			// --- Graceful Bounds Enforcement (Reflection) ---
			int minY = 60;
			int maxY = screenHeight - 40;

			// Reflect off the ceiling
			if (surfaceY < minY) {
			    surfaceY = minY + (minY - surfaceY);
			}
			// Reflect off the floor
			if (surfaceY > maxY) {
			    surfaceY = maxY - (surfaceY - maxY);
			}

			// Safety double-check in case Tier 3 variance is utterly massive
			surfaceY = Math.clamp(surfaceY, minY, maxY);

			// Render column
			for (int y = surfaceY; y < screenHeight; y++) {
				solidGrid[x][y] = true;
			}
		}
	}

	/**
	 * PER-FRAME UPDATE: Shifts floating dirt down by GRAVITY pixels.
	 * @return true if terrain is still falling/settling, false if completely stable.
	 */
	public boolean update() {
	    boolean terrainMoved = false;
	    for (int x = 0; x < screenWidth; x++) {
	        for (int y = screenHeight - 1 - GRAVITY; y >= 0; y--) {
	            if (solidGrid[x][y]) {
	                // Find how far we can fall, up to GRAVITY pixels
	                int fallDistance = 0;
	                for (int d = 1; d <= GRAVITY; d++) {
	                    if (!solidGrid[x][y + d]) {
	                        fallDistance = d;
	                    } else {
	                        break;
	                    }
	                }
	                if (fallDistance > 0) {
	                    solidGrid[x][y + fallDistance] = true;
	                    solidGrid[x][y] = false;
	                    terrainMoved = true; // Terrain is still collapsing
	                }
	            }
	        }
	    }
	    return terrainMoved;
	}

	/**
	 * Renders the terrain by drawing optimized vertical spans of dirt.
	 */
	public void draw(Graphics2D g2d) {
		for (int x = 0; x < screenWidth; x++) {
			int spanStart = -1;

			for (int y = 0; y < screenHeight; y++) {
				if (solidGrid[x][y]) {
					if (spanStart == -1) {
						spanStart = y;
						
						// Green grass on top of any exposed upper layer
						g2d.setColor(Color.GREEN);
						g2d.fillRect(x, spanStart, 1, Math.min(4, screenHeight - spanStart));
						g2d.setColor(dirtColor);
					}
				} else {
					if (spanStart != -1) {
						int drawStart = Math.min(spanStart + 4, screenHeight);
						if (y > drawStart) {
							g2d.drawLine(x, drawStart, x, y - 1);
						}
						spanStart = -1;
					}
				}
			}
			if (spanStart != -1) {
				int drawStart = Math.min(spanStart + 4, screenHeight);
				if (screenHeight - 1 >= drawStart) {
					g2d.drawLine(x, drawStart, x, screenHeight - 1);
				}
			}
		}
	}

	/**
	 * Scans down to find the highest solid ground level at column x.
	 */
	public int getHeightAt(int x) {
		if (x < 0) x = 0;
		if (x >= screenWidth) x = screenWidth - 1;

		for (int y = 0; y < screenHeight; y++) {
			if (solidGrid[x][y]) {
				return y;
			}
		}
		return screenHeight - 1;
	}

	/**
	 * Destroys a circle of pixels. Gravity calculations are handled entirely 
	 * by the update loop over subsequent frames.
	 */
	public void explode(int centerX, int centerY, int radius) {
		int startX = Math.max(0, centerX - radius);
		int endX = Math.min(screenWidth - 1, centerX + radius);
		int startY = Math.max(0, centerY - radius);
		int endY = Math.min(screenHeight - 1, centerY + radius);

		for (int x = startX; x <= endX; x++) {
			for (int y = startY; y <= endY; y++) {
				int dx = x - centerX;
				int dy = y - centerY;
				
				if ((dx * dx) + (dy * dy) <= (radius * radius)) {
					solidGrid[x][y] = false; 
				}
			}
		}
	}

	/**
	 * Checks if the terrain is solid at the given coordinates.
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isSolidAt(double x, double y) {
		return solidGrid[(int) x][(int) y];
	}
	
	
	public int getScreenWidth() {
		return screenWidth;
	}
}