package scorched.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import scorched.sound.SoundEngine;

public class WeatherManager {

    public enum WeatherType {
        CLEAR, RAIN, SNOW, STORMY, SANDSTORM, METEOR_SHOWER
    }

    private WeatherType currentType = WeatherType.CLEAR;
    private final int screenWidth;
    private final int screenHeight;
    private final Random rand = new Random();
    
    // Lightning strike trackers
    private boolean drawBolt = false;
    private int strikeX = 0;
    private int strikeY = 0;
    private List<java.awt.Point> boltPoints = new ArrayList<>();
    private boolean strikeTriggeredThisFrame = false;
    
    // Lightning mechanics
    private int lightningTimer = 0;
    private boolean isLightningFlashing = false;
    private int flashDuration = 0;

    // Particle pool
    private final List<WeatherParticle> particles = new ArrayList<>();
    // Tail smoke particle pool for foreground meteors
    private final List<SmokeParticle> smokeParticles = new ArrayList<>();

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
            } else if (currentType == WeatherType.SANDSTORM) {
                // Spawn across the screen on frame 1, otherwise feed from the left edge
                double spawnX = particles.isEmpty() ? rand.nextInt(screenWidth) : 0;
                
                // High horizontal speed (6 to 11), with a tiny bit of vertical jitter (-1 to 2)
                double vx = rand.nextInt(6) + 6;
                double vy = rand.nextGaussian() * 0.8 + 0.5; 
                
                particles.add(new WeatherParticle(spawnX, rand.nextInt(screenHeight), vx, vy, WeatherType.SANDSTORM));
            } else if (currentType == WeatherType.METEOR_SHOWER) {
                // Spawn background dressing meteors at a steady rate
            	if (rand.nextInt(5) == 0) {
                    boolean fallFromLeft = rand.nextBoolean();
                    double spawnX, vx;
                    
                    if (fallFromLeft) {
                        spawnX = rand.nextInt(screenWidth + 200) - 200; // Left side / off-screen left
                        vx = rand.nextInt(3) + 6;                       // Moves right (positive vx)
                    } else {
                        spawnX = rand.nextInt(screenWidth + 200);       // Right side / off-screen right
                        vx = -(rand.nextInt(3) + 6);                    // Moves left (negative vx)
                    }
                    
                    double vy = rand.nextInt(3) + 6; // Always falls downward
                    particles.add(new WeatherParticle(spawnX, 0, vx, vy, WeatherType.METEOR_SHOWER, false));
                }
                
                // Spawn a gameplay-impacting foreground meteor occasionally (approx. once every 4-5 seconds at 30 FPS)
            	if (rand.nextInt(130) == 0) {
                    boolean fallFromLeft = rand.nextBoolean();
                    double spawnX, vx;
                    
                    if (fallFromLeft) {
                        spawnX = rand.nextInt(screenWidth - 200);
                        vx = rand.nextInt(2) + 5; // Moves right
                    } else {
                        spawnX = rand.nextInt(screenWidth - 200) + 200;
                        vx = -(rand.nextInt(2) + 5); // Moves left
                    }
                    
                    double vy = rand.nextInt(2) + 5; // Always falls downward
                    particles.add(new WeatherParticle(spawnX, 0, vx, vy, WeatherType.METEOR_SHOWER, true));
                }
            }
        }

        // 2. Particle Physics & Collision
        for (int i = particles.size() - 1; i >= 0; i--) {
            WeatherParticle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            
            // Spawn trailing smoke particles for active foreground meteors
            if (p.type == WeatherType.METEOR_SHOWER && p.isForeground) {
                smokeParticles.add(new SmokeParticle(p.x, p.y));
            }
            
            // Sandstorms should also check if they blow past the right border
            boolean outOfBounds = p.y >= screenHeight || p.x >= screenWidth || p.x < 0;

            boolean hitGround = terrain.isSolidAt((int) p.x, (int) p.y);

            // Check if particle hits solid ground or leaves screen boundaries
            if (outOfBounds || hitGround) {
            	// If it's a foreground meteor hitting solid ground inside boundaries, trigger a terrain explosion event
                if (p.type == WeatherType.METEOR_SHOWER && p.isForeground && hitGround && p.x >= 0 && p.x < screenWidth) {
                    strikeX = (int) p.x;
                    strikeY = (int) p.y;
                    strikeTriggeredThisFrame = true;
                    SoundEngine.playMeteorStrikeSound();
                }
                
                particles.remove(i);
            }
        }
        
        // Update smoke trail life tracking
        for (int i = smokeParticles.size() - 1; i >= 0; i--) {
            SmokeParticle sp = smokeParticles.get(i);
            sp.life--;
            if (sp.life <= 0) {
                smokeParticles.remove(i);
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
        
        // Draw ambient dusty haze for sandstorms to reduce overall visibility
        if (currentType == WeatherType.SANDSTORM) {
            g2d.setColor(new Color(210, 180, 140, 45)); // Soft desert tan overlay
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        }
        
        // Draw smoke particle trails first (so they sit underneath the fiery streak head)
        for (SmokeParticle sp : smokeParticles) {
            int alpha = (int) ((sp.life / 15.0) * 120); // Fade out over time
            g2d.setColor(new Color(90, 85, 85, Math.max(0, Math.min(255, alpha))));
            g2d.fillOval((int) sp.x - 2, (int) sp.y - 2, 4, 4);
        }

        // Render individual droplets/flakes
        for (WeatherParticle p : particles) {
            if (p.type == WeatherType.RAIN) {
                g2d.setColor(new Color(150, 190, 255, 160));
                g2d.drawLine((int) p.x, (int) p.y, (int) (p.x - p.vx), (int) (p.y - 3));
            } else if (p.type == WeatherType.SNOW) {
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRect((int) p.x, (int) p.y, 2, 2);
            } else if (currentType == WeatherType.SANDSTORM) {
                // Warm, sandy color palette with randomized opacity per particle
                g2d.setColor(new Color(220, 185, 130, rand.nextInt(120) + 135));
                
                // Draw as a small dot (alternating 1x1 and 2x2 sizes)
                int size = (rand.nextBoolean()) ? 1 : 2;
                g2d.fillRect((int) p.x, (int) p.y, size, size);
            } else if (p.type == WeatherType.METEOR_SHOWER) {
                if (p.isForeground) {
                    // Thick, prominent foreground streak
                    g2d.setColor(new Color(255, 90, 0, 230)); // Fiery Orange
                    g2d.setStroke(new java.awt.BasicStroke(3f));
                    g2d.drawLine((int) p.x, (int) p.y, (int) (p.x - p.vx * 1.5), (int) (p.y - p.vy * 1.5));
                    g2d.setStroke(new java.awt.BasicStroke(1f));
                    
                    // Core brightness point
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval((int) p.x - 2, (int) p.y - 2, 4, 4);
                } else {
                    // Fainter, thin background cosmetic streak
                    g2d.setColor(new Color(240, 110, 20, 100)); 
                    g2d.drawLine((int) p.x, (int) p.y, (int) (p.x - p.vx), (int) (p.y - p.vy));
                }
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
        boolean isForeground;

        WeatherParticle(double x, double y, double vx, double vy, WeatherType type) {
            this(x, y, vx, vy, type, false);
        }

        WeatherParticle(double x, double y, double vx, double vy, WeatherType type, boolean isForeground) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.type = type;
            this.isForeground = isForeground;
        }
    }
    
    // Small data holder representing trail particles
    private static class SmokeParticle {
        double x, y;
        int life = 15; // Frames until dissolution

        SmokeParticle(double x, double y) {
            this.x = x;
            this.y = y;
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
    
    public boolean hasStrikeImpacted() { 
        return (currentType == WeatherType.STORMY && drawBolt && isLightningFlashing) || 
               (currentType == WeatherType.METEOR_SHOWER && strikeTriggeredThisFrame); 
    }
    
    public int getStrikeX() { return strikeX; }
    public int getStrikeY() { return strikeY; }
}