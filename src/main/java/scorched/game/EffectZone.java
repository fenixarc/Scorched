package scorched.game;

import java.awt.Graphics2D;

public abstract class EffectZone {
    protected double x, y;
    protected int radius;
    protected int turnsRemaining;
    protected int damagePerTurn;

    public EffectZone(double x, double y, int radius, int turnsRemaining, int damagePerTurn) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.turnsRemaining = turnsRemaining;
        this.damagePerTurn = damagePerTurn;
    }

    public abstract void draw(Graphics2D g);
    public abstract void applyEffect(Tank tank);

    public boolean isExpired() {
        return turnsRemaining <= 0;
    }

    public void decrementTurn() {
        turnsRemaining--;
    }

    public boolean contains(double tankX, double tankY) {
        double dx = tankX - x;
        double dy = tankY - y;
        return Math.sqrt(dx * dx + dy * dy) <= radius;
    }
}
