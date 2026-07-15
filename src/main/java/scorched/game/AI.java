package scorched.game;

import java.util.List;
import java.util.Random;

import scorched.game.GameEngine.GameState;
import scorched.weapons.AmmoType;

public class AI {
    private int difficultyLevel; // 1 to 5
    private double money;
    private Random random;
    private Tank myTank;

    // Tracks how many consecutive shots we've fired at the current target
    private int shotsFiredAtTarget;
    private Tank currentTarget;

    public AI(int difficultyLevel, double startingMoney, Tank myTank) {
        this.difficultyLevel = Math.max(1, Math.min(5, difficultyLevel));
        this.money = startingMoney;
        this.random = new Random();
        this.shotsFiredAtTarget = 0;
        this.myTank = myTank;
    }

    /**
     * Main turn execution method.
     */
    public void takeTurn(GameState gameState, Terrain terrain, List<Tank> activePlayers) {
        // 1. Future Phase: Buy weapons if needed
    	if (gameState == GameState.BUYING)
    		shopPhase(gameState);

    	else if (gameState == GameState.PLAYING) {
    		
	        // 2. Future Phase: Choose the best weapon for the situation
	        AmmoType chosenAmmoType = selectAmmoType();
	
	        // 3. Target Selection
	        Tank target = selectTarget(activePlayers);
	        if (target != null) {
	            // Reset shot count if target changed
	            if (target != this.currentTarget) {
	                this.currentTarget = target;
	                this.shotsFiredAtTarget = 0;
	            }
	
	            // 4. Aiming and Firing
	            fireShot(gameState, terrain, target, chosenAmmoType);
	            this.shotsFiredAtTarget++;
	        }
    	}
    }
    
    /**
     * Stub method for future AI shopping phase
     */
    private void shopPhase(GameState gameState) {
        if (this.money < 1000) return; // Too poor

        // AI Economy behavior based on difficulty
        if (this.difficultyLevel >= 4) {
            // Smart AI buys shields and tier-3 weapons (e.g., MIRVs, Death Heads)
            //if (!inventory.hasItem("Shield")) {
            //    this.money -= inventory.buyItem("Shield");
            //}
        } else if (this.difficultyLevel == 3) {
            // Medium AI buys standard upgrades (e.g., Baby Nukes)
            //this.money -= inventory.buyItem("BabyNuke");
        }
        // Level 1-2 AI might just hoard money or buy random things
    }
    
    /**
     * Selects which AmmoType the AI should fire.
     * Currently just fires HERound.
     */
    private AmmoType selectAmmoType() {
        // If target is far away, look for a large AoE weapon
        if (this.difficultyLevel >= 3 && shotsFiredAtTarget > 2) {
            // We keep missing, let's switch to a weapon with a bigger blast radius
            return this.myTank.getInventory().getWeaponWithLargestExplosion();
        }
        
        // Default to standard weapon
        return this.myTank.getInventory().getDefaultAmmoType();
    }
    
    /**
     * AI selects target to fire at based on its difficultyLevel.
     * @param activePlayers
     * @return
     */
    private Tank selectTarget(List<Tank> activePlayers) {
        // Remove 'this' AI from the list of potential targets
        List<Tank> opponents = activePlayers.stream()
            .filter(t -> t.getPlayerIndex() != this.myTank.getPlayerIndex() && t.isAlive())
            .toList();

        if (opponents.isEmpty()) return null;

        // Easy AI (1-2): Picks a random target
        if (this.difficultyLevel <= 2) {
            return opponents.get(random.nextInt(opponents.size()));
        }

        // Hard AI (3-5): Targets the closest player
        Tank closest = opponents.get(0);
        double minDistance = Double.MAX_VALUE;
        
        for (Tank t : opponents) {
            double dist = Math.abs(t.getX() - this.myTank.getX());
            if (dist < minDistance) {
                minDistance = dist;
                closest = t;
            }
        }
        return closest;
    }
    
    /**
     * AI fires at selected target with selected ammo.
     * 
     * @param gameState
     * @param target
     * @param ammoType
     */
    private void fireShot(GameState gameState, Terrain terrain, Tank target, AmmoType ammoType) {
        double targetX = target.getX();
        double targetY = target.getY();
        double myX = this.myTank.getX();
        double myY = this.myTank.getY();

        // Calculate base angle based on direction
        double defaultAngle = (targetX > myX) ? 45.0 : 135.0; 
        
        // Calculate barrel tip coordinates 
        double angleRad = Math.toRadians(defaultAngle);
        int barrelLength = 20;
        double tipX = myX + Math.cos(angleRad) * barrelLength;
        double tipY = myY - Math.sin(angleRad) * barrelLength;
        
        double perfectPower = calculatePerfectPower(defaultAngle, targetX, targetY, tipX, tipY);
        
        // Simulate the shot to see if it hits terrain
        boolean clearPath = ProjectileSimulator.checkTrajectory(tipX, tipY, defaultAngle, perfectPower, terrain, target);
        
        // Level 2 and higher get to recalculate their shot
        if (!clearPath && this.difficultyLevel >= 2) {
        	defaultAngle = (targetX > myX) ? 0.0 : 180.0;
        	// Check every 10 degrees from max to 90
        	for (int i = 1; i < 9; i++) {
        		System.out.println("Simulated shot missed, revising");
        		defaultAngle += (targetX > myX) ? 10.0 : -10.0;
        		
        		// Recalculate tip positions for the new angle
                angleRad = Math.toRadians(defaultAngle);
                tipX = myX + Math.cos(angleRad) * barrelLength;
                tipY = myY - Math.sin(angleRad) * barrelLength;
                
                perfectPower = calculatePerfectPower(defaultAngle, targetX, targetY, tipX, tipY);
                
                clearPath = ProjectileSimulator.checkTrajectory(
                        tipX, tipY, defaultAngle, perfectPower, terrain, target
                    );
        		if (clearPath) break;
        	}
        }

        // Apply Difficulty Noise
        double noiseFactor = getNoiseFactor();
        
        // Scale down the error the more shots we take at this target
        double adjustmentSpread = Math.max(0.1, 1.0 - (shotsFiredAtTarget * 0.3)); 
        
        // Add random gaussian noise to angle and power
        double angleNoise = random.nextGaussian() * noiseFactor * adjustmentSpread * 15.0; 
        double powerNoise = random.nextGaussian() * noiseFactor * adjustmentSpread * 4.0; 

        // Account for wind (default to 0 for now)
        double windAdjustment = 0;

        // Calculate raw values
        int rawAngle = (int) (defaultAngle + angleNoise);
        double rawPower = perfectPower + powerNoise + windAdjustment;

        // Clamp the values safely within legal game constraints before setting them
        int finalAngle = Math.max(0, Math.min(180, rawAngle));
        
        // Handle potential NaN fallback gracefully and clamp power between tank MIN and MAX
        if (Double.isNaN(rawPower)) {
            rawPower = Tank.DEFAULT_POWER; // Standard fallback power
        }
        double finalPower = Math.max(Tank.MIN_POWER, Math.min(Tank.MAX_POWER, rawPower));

        // Fire the weapon safely
        //this.myTank.setBarrelAngle(finalAngle);
        //this.myTank.setPower(finalPower);
        this.myTank.setBarrelAngle(finalAngle);
        this.myTank.setPower(finalPower);
        this.myTank.setCurrentAmmoType(ammoType);
        System.out.println("AI: " + this.myTank.getName() + " targeting: " + target.getName() + " firing: " + ammoType.getName() 
        		+ " perfectPower: " + (float) perfectPower + " finalPower: " + (float) finalPower 
        		+ " defaultAngle: " + defaultAngle + " finalAngle: " + finalAngle);
    }
    
    private double calculatePerfectPower(double defaultAngle, double targetX, double targetY, double tipX, double tipY) {
        double angleRad = Math.toRadians(defaultAngle);

        // Calculate displacement relative to the provided barrel tip
        double x = targetX - tipX; 
        double y = targetY - tipY; 
        double gravity = 0.15;

        double cosAngle = Math.cos(angleRad);
        double tanAngle = Math.tan(angleRad);

        // Calculate perfect power using inverted Y physics
        // Screen Trajectory: y = -x * tan(θ) + (gravity * x^2) / (2 * v^2 * cos^2(θ))
        double denominator = 2 * (cosAngle * cosAngle) * (y + x * tanAngle);

        if (denominator > 0) {
        	// Calculate the physical velocity required
            return Math.sqrt((gravity * x * x) / denominator); 
        } else {
        	// If the denominator is negative or zero, the target is physically unreachable.
            // Fallback to a baseline power.
            return 10.0;
        }
    }

    private double getNoiseFactor() {
        switch (this.difficultyLevel) {
            case 1: return 1.0;  // Huge errors
            case 2: return 0.5;  // Generous errors
            case 3: return 0.2;  // Decent aim
            case 4: return 0.05; // Sharp shooters
            case 5: return 0.0;  // Laser accurate
            default: return 1.0;
        }
    }
    
    public void addMoney(double amount) {
        this.money += amount;
    }
}