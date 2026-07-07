package scorched.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

public class Terrain {

	// Changed from boolean[][] to Color[][] to store persistent color data per pixel
	private Color[][] terrainGrid;
	private int screenWidth;
	private int screenHeight;
	private Color dirtColor;

	private static final int GRAVITY = 4;

	public Terrain(int screenWidth, int screenHeight, Color dirtColor, int hillStrength) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.terrainGrid = new Color[screenWidth][screenHeight]; // null represents empty sky/air
		this.dirtColor = dirtColor;

		generateTerrain(hillStrength);
	}

	/**
	 * Generates a randomized set of rolling hills and fills the 2D grid with layered shades.
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

		double largeHillsVariance = screenHeight * (0.08 + rand.nextDouble() * 0.12); // Large hills height variance
		double mediumHillsVariance = screenHeight * (0.02 + rand.nextDouble() * 0.04); // Medium hills height variance
		double jaggednessIntensity = 3 + rand.nextInt(6);  // Jaggedness intensity
		
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

		// Define persistent shade variations based on altitude tiers
		Color grassColor = new Color(34, 139, 34);
		Color lightShade = dirtColor.brighter().brighter();
		Color mediumShade = dirtColor.brighter();
		Color darkShade = dirtColor;
		Color deepShade = dirtColor.darker();

		// Calculate the height array column-by-column
		for (int x = 0; x < screenWidth; x++) {
			double wave1 = Math.sin((x + offset1) * freq1) * largeHillsVariance;
			double wave2 = Math.sin((x + offset2) * freq2) * mediumHillsVariance;
			double wave3 = Math.cos((x + offset3) * freq3) * jaggednessIntensity;

			// Combine calculations into the column height index
			int surfaceY = (int) (baselineY - (wave1 + wave2 + wave3));

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

			// Render column using deep-relative shading bands
			for (int y = surfaceY; y < screenHeight; y++) {
			    int depth = y - surfaceY; // Distance from the very top of the hill at this column

			    if (depth < 4) {
			        terrainGrid[x][y] = grassColor;  // Top 4 pixels are always grass
			    } else if (depth < 60) {
			        terrainGrid[x][y] = lightShade;  // Subsurface dirt layer
			    } else if (depth < 140) {
			        terrainGrid[x][y] = mediumShade; // Mid-depth soil
			    } else if (depth < 300) {
			        terrainGrid[x][y] = darkShade;   // Deep underground layer
			    } else {
			        terrainGrid[x][y] = deepShade;   // Core bedrock layer
			    }
			}
		}
	}

	/**
	 * PER-FRAME UPDATE: Shifts floating dirt down by GRAVITY pixels, carrying its Color property with it.
	 * @return true if terrain is still falling/settling, false if completely stable.
	 */
	public boolean update() {
	    boolean terrainMoved = false;
	    for (int x = 0; x < screenWidth; x++) {
	        for (int y = screenHeight - 1 - GRAVITY; y >= 0; y--) {
	            if (terrainGrid[x][y] != null) {
	            	// Find how far we can fall, up to GRAVITY pixels
	                int fallDistance = 0;
	                for (int d = 1; d <= GRAVITY; d++) {
	                    if (terrainGrid[x][y + d] == null) {
	                        fallDistance = d;
	                    } else {
	                        break;
	                    }
	                }
	                if (fallDistance > 0) {
	                    // The color physical property moves down to the new location
	                    terrainGrid[x][y + fallDistance] = terrainGrid[x][y];
	                    terrainGrid[x][y] = null;
	                    terrainMoved = true; // Terrain is still collapsing
	                }
	            }
	        }
	    }
	    return terrainMoved;
	}

	/**
	 * Renders the terrain by grouping sequential pixels of identical colors into single line-draw batches.
	 */
	public void draw(Graphics2D g2d) {
		for (int x = 0; x < screenWidth; x++) {
			int spanStart = -1;
			Color currentSpanColor = null;

			for (int y = 0; y < screenHeight; y++) {
				Color pixelColor = terrainGrid[x][y];

				if (pixelColor != null) {
					if (spanStart == -1) {
						spanStart = y;
						currentSpanColor = pixelColor;
					} else if (!pixelColor.equals(currentSpanColor)) {
						// Flush line segment because color changed
						g2d.setColor(currentSpanColor);
						g2d.drawLine(x, spanStart, x, y - 1);
						spanStart = y;
						currentSpanColor = pixelColor;
					}
				} else {
					if (spanStart != -1) {
						// Flush line segment because we hit empty space
						g2d.setColor(currentSpanColor);
						g2d.drawLine(x, spanStart, x, y - 1);
						spanStart = -1;
						currentSpanColor = null;
					}
				}
			}
			// Final flush for the bottom boundary of the screen
			if (spanStart != -1) {
				g2d.setColor(currentSpanColor);
				g2d.drawLine(x, spanStart, x, screenHeight - 1);
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
			if (terrainGrid[x][y] != null) {
				return y;
			}
		}
		return screenHeight - 1;
	}

	/**
	 * Destroys a circle of pixels, leaving behind an empty space (null).
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
					terrainGrid[x][y] = null; 
				}
			}
		}
	}

	/**
	 * Checks if the terrain is solid at the given coordinates.
	 */
	public boolean isSolidAt(double x, double y) {
		int ix = (int) x;
		int iy = (int) y;
		if (ix < 0 || ix >= screenWidth || iy < 0 || iy >= screenHeight) {
			return false;
		}
		return terrainGrid[ix][iy] != null;
	}
	
	public int getScreenWidth() {
		return screenWidth;
	}
}