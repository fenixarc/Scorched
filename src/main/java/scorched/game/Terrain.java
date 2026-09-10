package scorched.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

public class Terrain {

	private Color[][] terrainGrid;
	private int screenWidth;
	private int screenHeight;
	private Color dirtColor;
	private boolean isCollapsing = false;

	private static final int GRAVITY = 4;

	public Terrain(int screenWidth, int screenHeight, Color dirtColor, int hillStrength) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.terrainGrid = new Color[screenWidth][screenHeight];
		this.dirtColor = dirtColor;

		generateTerrain(hillStrength);
	}

	private void generateTerrain(int hillStrength) {
		Random rand = new Random();
		double randomBaselinePercent = 0.55 + (rand.nextDouble() * 0.20);
		int baselineY = (int) (screenHeight * randomBaselinePercent);
		double offset1 = rand.nextDouble() * 10000;
		double offset2 = rand.nextDouble() * 10000;
		double offset3 = rand.nextDouble() * 10000;
		double largeHillsVariance = screenHeight * (0.08 + rand.nextDouble() * 0.12);
		double mediumHillsVariance = screenHeight * (0.02 + rand.nextDouble() * 0.04);
		double jaggednessIntensity = 3 + rand.nextInt(6);
		double freq1 = 0.002;
		double freq2 = 0.01;
		double freq3 = 0.03;

		if (hillStrength == 2) {
			largeHillsVariance *= 2.0;
			mediumHillsVariance *= 2.0;
			freq1 *= 0.7;
		} else if (hillStrength == 3) {
			largeHillsVariance *= 3.0;
			mediumHillsVariance *= 2.5;
			jaggednessIntensity *= 8.0;
			freq1 *= 1.5;
			freq3 *= 1.2;
		}

		Color grassColor = new Color(34, 139, 34);
		Color lightShade = dirtColor.brighter().brighter();
		Color mediumShade = dirtColor.brighter();
		Color darkShade = dirtColor;
		Color deepShade = dirtColor.darker();

		for (int x = 0; x < screenWidth; x++) {
			double wave1 = Math.sin((x + offset1) * freq1) * largeHillsVariance;
			double wave2 = Math.sin((x + offset2) * freq2) * mediumHillsVariance;
			double wave3 = Math.cos((x + offset3) * freq3) * jaggednessIntensity;
			int surfaceY = (int) (baselineY - (wave1 + wave2 + wave3));
			int minY = 60;
			int maxY = screenHeight - 40;
			if (surfaceY < minY) surfaceY = minY + (minY - surfaceY);
			if (surfaceY > maxY) surfaceY = maxY - (surfaceY - maxY);
			surfaceY = Math.clamp(surfaceY, minY, maxY);

			for (int y = surfaceY; y < screenHeight; y++) {
				int depth = y - surfaceY;
				if (depth < 4) terrainGrid[x][y] = grassColor;
				else if (depth < 60) terrainGrid[x][y] = lightShade;
				else if (depth < 140) terrainGrid[x][y] = mediumShade;
				else if (depth < 300) terrainGrid[x][y] = darkShade;
				else terrainGrid[x][y] = deepShade;
			}
		}
	}

	public boolean update() {
		boolean terrainMoved = false;
		for (int x = 0; x < screenWidth; x++) {
			for (int y = screenHeight - 1 - GRAVITY; y >= 0; y--) {
				if (terrainGrid[x][y] != null) {
					int fallDistance = 0;
					for (int d = 1; d <= GRAVITY; d++) {
						if (terrainGrid[x][y + d] == null) fallDistance = d;
						else break;
					}
					if (fallDistance > 0) {
						terrainGrid[x][y + fallDistance] = terrainGrid[x][y];
						terrainGrid[x][y] = null;
						terrainMoved = true;
					}
				}
			}
		}
		isCollapsing = terrainMoved;
		return terrainMoved;
	}

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
						g2d.setColor(currentSpanColor);
						g2d.drawLine(x, spanStart, x, y - 1);
						spanStart = y;
						currentSpanColor = pixelColor;
					}
				} else if (spanStart != -1) {
					g2d.setColor(currentSpanColor);
					g2d.drawLine(x, spanStart, x, y - 1);
					spanStart = -1;
					currentSpanColor = null;
				}
			}
			if (spanStart != -1) {
				g2d.setColor(currentSpanColor);
				g2d.drawLine(x, spanStart, x, screenHeight - 1);
			}
		}
	}

	public int getHeightAt(int x) {
		if (x < 0) x = 0;
		if (x >= screenWidth) x = screenWidth - 1;
		for (int y = 0; y < screenHeight; y++) if (terrainGrid[x][y] != null) return y;
		return screenHeight - 1;
	}

	public void explode(int centerX, int centerY, int radius) {
		int startX = Math.max(0, centerX - radius);
		int endX = Math.min(screenWidth - 1, centerX + radius);
		int startY = Math.max(0, centerY - radius);
		int endY = Math.min(screenHeight - 1, centerY + radius);
		for (int x = startX; x <= endX; x++) {
			for (int y = startY; y <= endY; y++) {
				int dx = x - centerX;
				int dy = y - centerY;
				if ((dx * dx) + (dy * dy) <= (radius * radius)) terrainGrid[x][y] = null;
			}
		}
	}

	public boolean isSolidAt(double x, double y) {
		int ix = (int) x;
		int iy = (int) y;
		if (ix < 0 || ix >= screenWidth || iy < 0 || iy >= screenHeight) return false;
		return terrainGrid[ix][iy] != null;
	}

	public int getScreenWidth() { return screenWidth; }

	public boolean isCollapsing() { return isCollapsing; }
}
