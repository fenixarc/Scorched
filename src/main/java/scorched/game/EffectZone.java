package scorched.game;

import java.awt.Graphics2D;

public abstract class EffectZone {
    protected double x, y;
    protected int radius;
    protected int turnsRemaining;
    protected int damagePerTurn;
    protected String type;

    public EffectZone(double x, double y, int radius, int turnsRemaining, int damagePerTurn, String type) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.turnsRemaining = turnsRemaining;
        this.damagePerTurn = damagePerTurn;
        this.type = type;
    }

    public abstract void draw(Graphics2D g);

    public boolean isExpired() {
        return turnsRemaining == 0;
    }

    public void decrementTurn() {
        if (turnsRemaining > 0) {
            turnsRemaining--;
        }
    }

    public boolean contains(double tankX, double tankY) {
        double dx = tankX - x;
        double dy = tankY - y;
        return Math.sqrt(dx * dx + dy * dy) <= radius;
    }
}
