package scorched.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.junit.jupiter.api.Test;

public class AmmoTypeTest {

    @Test
    public void testAmmoTypeConstructorAndGetters() {
        // 1. Arrange: Set up the expected test data
        String expectedName = "Plasma Charge";
        int expectedRadius = 5;
        int expectedExplosionRadius = 25;
        int expectedDamage = 50;
        int expectedCost = 100;
        String expectedDescription = "A high-energy plasma blast that deals moderate area damage.";

        // 2. Act: Create an instance of AmmoType using the constructor
        AmmoType ammo = new AmmoType(
            expectedName, 
            expectedRadius, 
            expectedExplosionRadius, 
            expectedDamage, 
            expectedCost, 
            expectedDescription
        );

        // 3. Assert: Verify that the getters return the correct values
        // Using assertAll ensures all assertions run even if one fails, giving a complete report.
        assertAll("Verify all AmmoType properties are correctly initialized and retrieved",
            () -> assertEquals(expectedName, ammo.getName(), "Name should match the constructor input"),
            () -> assertEquals(expectedRadius, ammo.getRadius(), "Radius should match the constructor input"),
            () -> assertEquals(expectedExplosionRadius, ammo.getExplosionRadius(), "Explosion radius should match the constructor input"),
            () -> assertEquals(expectedDamage, ammo.getDamage(), "Damage should match the constructor input"),
            () -> assertEquals(expectedCost, ammo.getCost(), "Cost should match the constructor input"),
            () -> assertEquals(expectedDescription, ammo.getDescription(), "Description should match the constructor input")
        );
    }

    @Test
    public void testAmmoTypeWithBoundaryValues() {
        // Arrange & Act: Test with zero/negative/empty values to ensure the object holds data faithfully
        AmmoType freeZeroAmmo = new AmmoType("", 0, 0, -10, 0, null);

        // Assert
        assertAll("Verify AmmoType handles boundary/edge case values",
            () -> assertEquals("", freeZeroAmmo.getName()),
            () -> assertEquals(0, freeZeroAmmo.getRadius()),
            () -> assertEquals(0, freeZeroAmmo.getExplosionRadius()),
            () -> assertEquals(-10, freeZeroAmmo.getDamage()),
            () -> assertEquals(0, freeZeroAmmo.getCost()),
            () -> assertEquals(null, freeZeroAmmo.getDescription())
        );
    }

    @Test
    public void testAmmoTypeConstructorWithColor() {
        java.awt.Color color = java.awt.Color.RED;
        AmmoType ammo = new AmmoType("Red Ammo", 1, 1, 1, 1, "Desc", color);
        assertEquals(color, ammo.getProjectileColor());
    }

    @Test
    public void testAmmoTypeFullConstructor() {
        java.awt.Color color = java.awt.Color.BLUE;
        AmmoType ammo = new AmmoType("Full", 2, 3, 4, 5, "Desc", color, 6, 7, 8, 9);
        assertAll("Verify full constructor",
            () -> assertEquals("Full", ammo.getName()),
            () -> assertEquals(2, ammo.getRadius()),
            () -> assertEquals(3, ammo.getExplosionRadius()),
            () -> assertEquals(4, ammo.getDamage()),
            () -> assertEquals(5, ammo.getCost()),
            () -> assertEquals("Desc", ammo.getDescription()),
            () -> assertEquals(color, ammo.getProjectileColor()),
            () -> assertEquals(6, ammo.getEffectRadius()),
            () -> assertEquals(7, ammo.getEffectTurns()),
            () -> assertEquals(8, ammo.getEffectDamage()),
            () -> assertEquals(9, ammo.getRoundsFired())
        );
    }

}