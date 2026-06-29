import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

public class Terrain {

	private int[] heightMap;
	private int screenWidth;
	private int screenHeight;
	private Color dirtColor;

	public Terrain(int screenWidth, int screenHeight, Color dirtColor) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.heightMap = new int[screenWidth];
		this.dirtColor = dirtColor;

		generateTerrain();
	}

	/**
	 * Generates a randomized set of rolling hills using random wave
	 * offsets and heights.
	 */
	private void generateTerrain() {
		Random rand = new Random();

		// Randomize the baseline ground level slightly (between 55% and 75% of screen height)
		double randomBaselinePercent = 0.55 + (rand.nextDouble() * 0.20);
		int baselineY = (int) (screenHeight * randomBaselinePercent);

		// Generate random offsets so waves shift horizontally left or right uniquely
		double offset1 = rand.nextDouble() * 10000;
		double offset2 = rand.nextDouble() * 10000;
		double offset3 = rand.nextDouble() * 10000;

		// Randomize wave vertical strengths (amplitudes)
		double amp1 = screenHeight * (0.08 + rand.nextDouble() * 0.12); // Large hills height variance
		double amp2 = screenHeight * (0.02 + rand.nextDouble() * 0.04); // Medium hills height variance
		double amp3 = 3 + rand.nextInt(6); // Jaggedness intensity

		// Calculate the height array column-by-column
		for (int x = 0; x < screenWidth; x++) {
			// Large rolling peaks
			double wave1 = Math.sin((x + offset1) * 0.002) * amp1;

			// Secondary hills
			double wave2 = Math.sin((x + offset2) * 0.01) * amp2;

			// Jagged terrain texture
			double wave3 = Math.cos((x + offset3) * 0.03) * amp3;

			// Combine calculations into the column height index
			heightMap[x] = (int) (baselineY - (wave1 + wave2 + wave3));

			// Safety boundary protections
			if (heightMap[x] >= screenHeight)
				heightMap[x] = screenHeight - 1;
			if (heightMap[x] < 150)
				heightMap[x] = 150; // Don't let mountains reach all the way to the top UI
		}
	}

	/**
	 * Renders the terrain column by column from its top height down to the bottom
	 * of the screen.
	 */
	public void draw(Graphics2D g2d) {
		g2d.setColor(dirtColor);

		for (int x = 0; x < screenWidth; x++) {
			// Draw a vertical line from the top of the terrain to the bottom of the window
			g2d.drawLine(x, heightMap[x], x, screenHeight);
		}

		// Add a green grass line right along the very top edge of the hills
		g2d.setColor(Color.GREEN);
		for (int x = 0; x < screenWidth; x++) {
			g2d.fillRect(x, heightMap[x], 1, 4); // Draw a 4-pixel thick blade of grass
		}
	}

	// Getter so tanks can read the terrain height to place themselves on top of it
	public int getHeightAt(int x) {
		if (x < 0)
			return heightMap[0];
		if (x >= screenWidth)
			return heightMap[screenWidth - 1];
		return heightMap[x];
	}

	/**
	 * Modifies the heightmap to create a circular crater upon missile impact.
	 */
	public void explode(int centerX, int centerY, int radius) {
		// Determine bounds so we don't look outside the array bounds
		int startX = Math.max(0, centerX - radius);
		int endX = Math.min(screenWidth - 1, centerX + radius);

		for (int x = startX; x <= endX; x++) {
			// Find the horizontal distance from center of impact
			int dx = x - centerX;

			// Calculate the depth of the circular cutout at this specific vertical column
			// using the Pythagorean theorem
			// Equation of circle: dx^2 + dy^2 = radius^2 -> dy = sqrt(radius^2 - dx^2)
			double semiCircleY = Math.sqrt((radius * radius) - (dx * dx));

			// The bottom absolute point of the explosion circle
			int explosionBottomY = centerY + (int) semiCircleY;

			// If the bottom boundary of the explosion penetrates deeper than current
			// terrain surface, drop the earth down
			if (explosionBottomY > heightMap[x]) {
				// Drop ground level to the bottom edge of the explosion crater
				heightMap[x] = explosionBottomY;

				// Safety cap: Make sure it doesn't push the ground below screen layout limit
				if (heightMap[x] >= screenHeight) {
					heightMap[x] = screenHeight - 1;
				}
			}
		}
	}
}