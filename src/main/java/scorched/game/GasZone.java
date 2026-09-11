package scorched.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import scorched.sound.SoundEngine;

public class GasZone extends EffectZone {

    public GasZone(double x, double y, int radius, int turnsRemaining, int damagePerTurn) {
        super(x, y, radius, turnsRemaining, damagePerTurn);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.fillOval((int) (x - radius), (int) (y - radius), radius * 2, radius * 2);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    @Override
    public void applyEffect(Tank tank) {
        if (contains(tank.getX(), tank.getY())) {
        	SoundEngine.playAcidDamageSound();
            tank.takeDamage(damagePerTurn);
        }
    }
}
