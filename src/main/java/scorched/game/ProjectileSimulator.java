package scorched.game;

public class ProjectileSimulator {

    /**
     * Simulates a shot. Returns true if it hits the target area without hitting terrain first.
     */
    public static boolean checkTrajectory(double startX, double startY, double angle, double power, 
    									  Terrain terrain, Tank target) {
    	System.out.println("Simulating shot at power: " + (float) power + " angle: " + angle);
        double g = 0.15;
        
        // Convert angle to radians
        double rad = Math.toRadians(angle);
        
        // Horizontal velocity remains standard
        double vx = power * Math.cos(rad);
        
        // Subtracting power because moving UP means decreasing Y in screen space
        double vy = -power * Math.sin(rad); 

        double x = startX;
        double y = startY;
        double dt = .1; // Set step to 1.0 to perfectly match frame updates, or leave at 0.1 for precise paths

        // Simulate for a max of t seconds (or until out of bounds)
        for (double t = 0; t < 100.0; t += dt) {
            
            // Gravity pulls DOWNWARDS, meaning it INCREASES the screen Y-velocity
            vy += g * dt; 
            
            x += vx * dt;
            y += vy * dt;

            // 1. Check if we hit the left or right map boundaries
            if (x < 0 || x > terrain.getScreenWidth()) {
            	System.out.println("Simulated shot hit map boundary");
                return false; 
            }

            // 2. Check if we hit terrain (only check if it falls within the screen boundaries vertically)
            if (y >= 0 && terrain.isSolidAt(x, y)) {
                // Did we hit close enough to the target anyway?
                if (isCloseToTarget(x, y, target)) {
                	System.out.println("Simulated shot close enough");
                    return true;
                }
                System.out.println("Simulated shot hit terrain");
                return false; // Hit a mountain/terrain instead of the player
            }

            // 3. Check if we pass directly through the target's bounding box
            if (isCloseToTarget(x, y, target)) {
            	System.out.println("Simulated shot direct hit");
                return true;
            }
        }
        System.out.println("Simulated shot missed");
        return false;
    }

    private static boolean isCloseToTarget(double x, double y, Tank target) {
        double dx = x - target.getX();
        double dy = y - target.getY();
        return Math.sqrt(dx * dx + dy * dy) < 20.0; // 20-pixel hit radius
    }
}