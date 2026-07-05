package scorched.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scorched.weapons.AmmoType;
import scorched.weapons.HERound;

public class Inventory {
    // Maps the AmmoType to the current quantity owned
    private final Map<AmmoType, Integer> ammoStock;

    /** 
     * Create default ammo stock with only HE Rounds
     */
    public Inventory() {
        this.ammoStock = new HashMap<>();
        ammoStock.put(new HERound(), 100);
    }

    /**
     * Adds ammo to the inventory when purchased.
     */
    public void addAmmo(AmmoType type, int amount) {
        if (amount <= 0) return;
        int currentCount = ammoStock.getOrDefault(type, 0);
        ammoStock.put(type, currentCount + amount);
    }

    /**
     * Consumes 1 ammo when fired. Returns true if successful.
     */
    public boolean consumeAmmo(AmmoType type) {
        int currentCount = ammoStock.getOrDefault(type, 0);
        if (currentCount > 0) {
            ammoStock.put(type, currentCount - 1);
            return true;
        }
        return false; // Out of ammo!
    }

    /**
     * Gets the current count for a specific ammo type.
     */
    public int getAmmoCount(AmmoType type) {
        return ammoStock.getOrDefault(type, 0);
    }
    
    /**
     * Returns a List of ammo types that the player currently owns (count > 0).
     */
    public List<AmmoType> getAmmoTypes() {
        List<AmmoType> availableAmmo = new ArrayList<>();
        for (Map.Entry<AmmoType, Integer> entry : ammoStock.entrySet()) {
            if (entry.getValue() > 0) {
                availableAmmo.add(entry.getKey());
            }
        }
        return availableAmmo;
    }
    
    /**
     * Searches the inventory and returns the default ammo type based on its name.
     * Returns null if the default ammo isn't found in the inventory.
     */
    public AmmoType getDefaultAmmoType() {
        for (AmmoType type : ammoStock.keySet()) {
            if ("HE ROUND".equalsIgnoreCase(type.getName())) {
                return type;
            }
        }
        return null; 
    }
    
    /**
     * Searches the inventory and returns the AmmoType with the largest explosion radius.
     * Returns null if the inventory is empty.
     */
    public AmmoType getWeaponWithLargestExplosion() {
        if (ammoStock.isEmpty()) {
            return null;
        }

        AmmoType largestExplosionWeapon = null;
        int maxRadius = -1; // Tracks the highest radius found so far

        for (AmmoType type : ammoStock.keySet()) {
            if (type.getExplosionRadius() > maxRadius) {
                maxRadius = type.getExplosionRadius();
                largestExplosionWeapon = type;
            }
        }

        return largestExplosionWeapon;
    }
}