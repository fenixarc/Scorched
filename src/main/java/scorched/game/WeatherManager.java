package scorched.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import scorched.sound.SoundEngine;

public class WeatherManager {

    public enum WeatherType {
        CLEAR, RAIN, SNOW, STORMY
    }

    private WeatherType currentType = WeatherType.CLEAR;
    private final int screenWidth;
    private final int screenHeight;
    private final Random rand = new Random();
    
    // Physical strike trackers
    private boolean drawBolt = false;
    private int strikeX = 0;
    private int strikeY = 0;
    private List<java.awt.Point> boltPoints = new ArrayList<>();
    private boolean strikeTriggeredThisFrame = false;

    // Particle pool
    private final List<WeatherParticle> particles = new ArrayList<>();
    
    // Lightning mechanics
    private int lightningTimer = 0;
    private boolean isLightningFlashing = false;
    private int flashDuration = 0;

    public WeatherManager(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    /**
     * Sets up a new random weather pattern for the round.
     */
    public void randomizeWeather() {
        particles.clear();
        isLightningFlashing = false;
        
        // Pick a random weather state
        WeatherType[] types = WeatherType.values();
        this.currentType = types[rand.nextInt(types.length)];
        System.out.println("Weather is " + this.currentType);
        
        // Configure lightning intervals for storms
        if (currentType == WeatherType.STORMY) {
            lightningTimer = rand.nextInt(300) + 300; // Frames between flashes
        }
    }

    /**
     * Core update logic run per frame.
     */
    public void update(Terrain terrain) {
        if (currentType == WeatherType.CLEAR) return;

        // 1. Particle Generation
        int spawnRate = currentType == WeatherType.STORMY ? 1 : 1;
        for (int i = 0; i < spawnRate; i++) {
            if (currentType == WeatherType.RAIN || currentType == WeatherType.STORMY) {
                particles.add(new WeatherParticle(rand.nextInt(screenWidth), 0, 
                        rand.nextInt(2) - 1, rand.nextInt(5) + 10, WeatherType.RAIN));
            } else if (currentType == WeatherType.SNOW) {
                particles.add(new WeatherParticle(rand.nextInt(screenWidth), 0, 
                        rand.nextGaussian() * 0.5, rand.nextInt(2) + 2, WeatherType.SNOW));
            }
        }

        // 2. Particle Physics & Collision
        for (int i = particles.size() - 1; i >= 0; i--) {
            WeatherParticle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;

            // Check if particle hits solid ground or leaves screen boundaries
            if (p.y >= screenHeight || terrain.isSolidAt(p.x, p.y)) {
                particles.remove(i);
            }
        }

        // 3. Lightning Flash Timers
        if (currentType == WeatherType.STORMY) {
            if (isLightningFlashing) {
                flashDuration--;
                if (flashDuration <= 0) {
                    isLightningFlashing = false;
                    drawBolt = false;
                    lightningTimer = rand.nextInt(450) + 300; // Wait for next strike
                }
            } else {
                lightningTimer--;
                if (lightningTimer <= 0) {
                    isLightningFlashing = true;
                    flashDuration = rand.nextInt(4) + 2; // Lasts 2 to 5 frames
                    SoundEngine.playThunderSound();
                    
                    // --- PHYSICAL LIGHTNING STRIKE LOGIC ---
                    // Pick a random horizontal spot on the screen
                    strikeX = rand.nextInt(screenWidth);
                    // Use Terrain's surface detection to find where it impacts
                    strikeY = terrain.getHeightAt(strikeX);
                    
                    // Generate jagged points from sky (y=0) to ground impact point
                    boltPoints.clear();
                    int curX = strikeX + (rand.nextInt(40) - 20); // random sky offset
                    int curY = 0;
                    boltPoints.add(new java.awt.Point(curX, curY));
                    
                    while (curY < strikeY) {
                        curY += rand.nextInt(30) + 20; // step down vertically
                        if (curY > strikeY) curY = strikeY;
                        
                        // Jagged horizontal zig-zag jitter
                        curX += rand.nextInt(31) - 15; 
                        boltPoints.add(new java.awt.Point(curX, curY));
                    }
                    drawBolt = true;
                    strikeTriggeredThisFrame = true;
                }
            }
        }
    }

    /**
     * Renders precipitation overlays and full-screen flashes.
     */
    public void draw(Graphics2D g2d) {
        // Draw fullscreen blinding white flash if lightning strikes
        if (isLightningFlashing) {
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        }

        // Render individual droplets/flakes
        for (WeatherParticle p : particles) {
            if (p.type == WeatherType.RAIN) {
                g2d.setColor(new Color(150, 190, 255, 160));
                g2d.drawLine((int) p.x, (int) p.y, (int) (p.x - p.vx), (int) (p.y - 3));
            } else if (p.type == WeatherType.SNOW) {
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRect((int) p.x, (int) p.y, 2, 2);
            }
        }
        
        // Render the jagged lightning bolt
        if (drawBolt && boltPoints.size() > 1) {
            g2d.setColor(new Color(255, 255, 255));
            g2d.setStroke(new java.awt.BasicStroke(3f)); // Make the core thick
            for (int i = 0; i < boltPoints.size() - 1; i++) {
                java.awt.Point p1 = boltPoints.get(i);
                java.awt.Point p2 = boltPoints.get(i + 1);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            g2d.setStroke(new java.awt.BasicStroke(1f)); // Reset brush stroke
        }
    }

    public WeatherType getCurrentType() {
        return currentType;
    }

    // Lightweight data holder for rain streaks/snow flakes
    private static class WeatherParticle {
        double x, y;
        double vx, vy;
        WeatherType type;

        WeatherParticle(double x, double y, double vx, double vy, WeatherType type) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.type = type;
        }
    }
    
    /**
     * Checks if a new strike just occurred. If true, consumes the event 
     * by resetting the flag so it doesn't fire multiple times.
     */
    public boolean consumeStrike() {
        if (strikeTriggeredThisFrame) {
            strikeTriggeredThisFrame = false;
            return true;
        }
        return false;
    }
    
    public boolean hasStrikeImpacted() { return drawBolt && isLightningFlashing; }
    public int getStrikeX() { return strikeX; }
    public int getStrikeY() { return strikeY; }
}