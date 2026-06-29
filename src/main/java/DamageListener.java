public interface DamageListener {
	
    // Fires when a tank is hit
    void onTankTakeDamage(int x, int y, int amount);
    
    // Fires when a tank dies
    void onTurretSpawned(TurretDebris debris);
}