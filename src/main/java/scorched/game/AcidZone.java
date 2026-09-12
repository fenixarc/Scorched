package scorched.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import scorched.sound.SoundEngine;

public class AcidZone extends EffectZone {

    private List<AcidDrop> drops;
    private Terrain terrain;
    private static int ACID_RADIUS = 5;

    private static class AcidDrop {
        int x, y;
        AcidDrop(int x, int y) { this.x = x; this.y = y; }
    }

    public AcidZone(double x, double y, int radius, int turnsRemaining, int damagePerTurn, Terrain terrain) {
        super(x, y, radius, turnsRemaining, damagePerTurn);
        this.terrain = terrain;
        this.drops = new ArrayList<>();
        generateAcidDrops((int)x, (int)y, radius);
    }

    private void generateAcidDrops(int centerX, int centerY, int radius) {
        Random rand = new Random();
        int numDrops = 100;
        for (int i = 0; i < numDrops; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double dist = rand.nextDouble() * radius;
            int dx = (int) (Math.cos(angle) * dist);
            int dy = (int) (Math.sin(angle) * dist);
            
            int targetX = centerX + dx;
            int targetY = centerY + dy;
            
            if (targetX >= 0 && targetX < terrain.getScreenWidth()) {
                int groundY = terrain.getHeightAt(targetX);
                if (Math.abs(targetY - groundY) < 50) {
                    drops.add(new AcidDrop(targetX, groundY));
                }
            }
        }
    }

    public void update() {
        for (AcidDrop drop : drops) {
            int currentGroundY = terrain.getHeightAt(drop.x);
            if (drop.y < currentGroundY) {
                drop.y = currentGroundY;
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.GREEN);
        for (AcidDrop drop : drops) {
            g.fillRect(drop.x, drop.y, ACID_RADIUS, ACID_RADIUS);
        }
    }

    @Override
    public void applyEffect(Tank tank) {
        for (AcidDrop drop : drops) {
            if (tank.checkHit(drop.x, drop.y, ACID_RADIUS)) {
            	SoundEngine.playAcidDamageSound();
                tank.takeDamage(damagePerTurn);
                break;
            }
        }
    }
}
