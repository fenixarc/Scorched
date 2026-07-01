package scorched.game;
import java.awt.Color;

/**
 * Simple container to bind a matching sky and dirt color profile together.
 */
class EnvironmentPalette {
	Color sky;
	Color dirt;

	public EnvironmentPalette(Color sky, Color dirt) {
		this.sky = sky;
		this.dirt = dirt;
	}
}