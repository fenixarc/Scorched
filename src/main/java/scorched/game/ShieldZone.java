package scorched.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class ShieldZone extends EffectZone {

    private int health = 50;
    private static final String TYPE = "SHIELD";
    public Tank getOwner() {
        return owner;
    }

    private Tank owner;

    public ShieldZone(Tank owner, int radius, int damagePerTurn) {
        super(owner.getX(), owner.getY(), radius, -1, damagePerTurn, TYPE);
        this.owner = owner;
    }

    public void absorbDamage(int damage) {
        this.health -= damage;
        if (this.health < 0) this.health = 0;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    @Override
    public void draw(Graphics2D g) {
        if (isDestroyed()) return;

        // Calculate alpha based on health (100% health = opaque, 0% = invisible)
        int alpha = (int) (255 * (health / 50.0));
        alpha = Math.max(0, Math.min(255, alpha));
        
        g.setColor(new Color(255, 255, 255, alpha));
        g.setStroke(new BasicStroke(5));
        g.drawOval(owner.getX() - radius, owner.getY() - radius, radius * 2, radius * 2);
        g.setStroke(new BasicStroke(1));
    }
}
