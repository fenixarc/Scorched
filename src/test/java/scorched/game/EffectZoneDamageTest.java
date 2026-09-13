package scorched.game;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EffectZoneDamageTest {

    private List<Tank> tanks;
    private Tank tank1;

    @BeforeEach
    void setUp() {
        tanks = new ArrayList<>();
        tank1 = mock(Tank.class);
        when(tank1.isAlive()).thenReturn(true);
        when(tank1.getX()).thenReturn(100);
        when(tank1.getY()).thenReturn(100);
        tanks.add(tank1);
    }

    @Test
    void testAggregatedDamageApplication() {
        // Create two zones of the same type "ACID"
        EffectZone zone1 = new AcidZone(100, 100, 50, 1, 10, mock(Terrain.class));
        EffectZone zone2 = new AcidZone(100, 100, 50, 1, 20, mock(Terrain.class));
        
        List<EffectZone> activeEffectZones = new ArrayList<>();
        activeEffectZones.add(zone1);
        activeEffectZones.add(zone2);

        // Logic under test (mimicking Phase F)
        java.util.Map<Tank, java.util.Map<String, Integer>> pendingDamage = new java.util.HashMap<>();
        for (EffectZone zone : activeEffectZones) {
            for (Tank t : tanks) {
                if (t.isAlive() && zone.contains(t.getX(), t.getY())) {
                    pendingDamage.putIfAbsent(t, new java.util.HashMap<>());
                    java.util.Map<String, Integer> tankEffects = pendingDamage.get(t);
                    tankEffects.put(zone.type, Math.max(tankEffects.getOrDefault(zone.type, 0), zone.damagePerTurn));
                }
            }
        }

        // Verify aggregation: should take max(10, 20) = 20
        assertEquals(1, pendingDamage.size());
        assertEquals(20, pendingDamage.get(tank1).get("ACID"));
        
        // Apply damage
        for (java.util.Map.Entry<Tank, java.util.Map<String, Integer>> entry : pendingDamage.entrySet()) {
            for (int damage : entry.getValue().values()) {
                entry.getKey().takeDamage(damage);
            }
        }

        verify(tank1, times(1)).takeDamage(20);
    }

    @Test
    void testMultipleEffectTypesAggregation() {
        // 2 ACID zones (10, 20) and 1 GAS zone (15)
        EffectZone acid1 = new AcidZone(100, 100, 50, 1, 10, mock(Terrain.class));
        EffectZone acid2 = new AcidZone(100, 100, 50, 1, 20, mock(Terrain.class));
        EffectZone gas1 = new GasZone(100, 100, 50, 1, 15);
        
        List<EffectZone> activeEffectZones = List.of(acid1, acid2, gas1);

        java.util.Map<Tank, java.util.Map<String, Integer>> pendingDamage = new java.util.HashMap<>();
        for (EffectZone zone : activeEffectZones) {
            for (Tank t : tanks) {
                if (t.isAlive() && zone.contains(t.getX(), t.getY())) {
                    pendingDamage.putIfAbsent(t, new java.util.HashMap<>());
                    java.util.Map<String, Integer> tankEffects = pendingDamage.get(t);
                    tankEffects.put(zone.type, Math.max(tankEffects.getOrDefault(zone.type, 0), zone.damagePerTurn));
                }
            }
        }

        // Verify aggregation: ACID=20, GAS=15
        assertEquals(20, pendingDamage.get(tank1).get("ACID"));
        assertEquals(15, pendingDamage.get(tank1).get("GAS"));
        
        // Apply damage
        for (java.util.Map.Entry<Tank, java.util.Map<String, Integer>> entry : pendingDamage.entrySet()) {
            for (int damage : entry.getValue().values()) {
                entry.getKey().takeDamage(damage);
            }
        }

        verify(tank1).takeDamage(20);
        verify(tank1).takeDamage(15);
        verify(tank1, times(2)).takeDamage(anyInt());
    }
}
