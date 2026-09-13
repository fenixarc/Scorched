package scorched.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;

public class GasZone extends EffectZone {
	
	private static String TYPE = "GAS";

    public GasZone(double x, double y, int radius, int turnsRemaining, int damagePerTurn) {
        super(x, y, radius, turnsRemaining, damagePerTurn, TYPE);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.fillOval((int) (x - radius), (int) (y - radius), radius * 2, radius * 2);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}
