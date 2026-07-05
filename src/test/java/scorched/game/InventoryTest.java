package scorched.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scorched.weapons.AmmoType;
import scorched.weapons.HERound;

class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        // Initializes with the default HERound (100 qty) as per the constructor
        inventory = new Inventory();
    }

    @Test
    void testConstructorInitializesWithHERound() {
        // Default constructor adds an HERound with 100 stock
        AmmoType defaultAmmo = inventory.getDefaultAmmoType();
        assertNotNull(defaultAmmo, "Inventory should contain a default HE Round");
        assertEquals(100, inventory.getAmmoCount(defaultAmmo));
    }

    @Test
    void testAddAmmo_NewAmmoType() {
        AmmoType mockAmmo = mock(AmmoType.class);
        
        inventory.addAmmo(mockAmmo, 5);
        
        assertEquals(5, inventory.getAmmoCount(mockAmmo));
    }

    @Test
    void testAddAmmo_ExistingAmmoType() {
        AmmoType mockAmmo = mock(AmmoType.class);
        
        inventory.addAmmo(mockAmmo, 5);
        inventory.addAmmo(mockAmmo, 10);
        
        assertEquals(15, inventory.getAmmoCount(mockAmmo));
    }

    @Test
    void testAddAmmo_ZeroOrNegativeAmount() {
        AmmoType mockAmmo = mock(AmmoType.class);
        
        inventory.addAmmo(mockAmmo, 0);
        assertEquals(0, inventory.getAmmoCount(mockAmmo));
        
        inventory.addAmmo(mockAmmo, -5);
        assertEquals(0, inventory.getAmmoCount(mockAmmo));
    }

    @Test
    void testConsumeAmmo_Success() {
        AmmoType mockAmmo = mock(AmmoType.class);
        inventory.addAmmo(mockAmmo, 2);
        
        boolean success = inventory.consumeAmmo(mockAmmo);
        
        assertTrue(success);
        assertEquals(1, inventory.getAmmoCount(mockAmmo));
    }

    @Test
    void testConsumeAmmo_Failure_OutOfAmmo() {
        AmmoType mockAmmo = mock(AmmoType.class); // Starts at 0 count
        
        boolean success = inventory.consumeAmmo(mockAmmo);
        
        assertFalse(success);
        assertEquals(0, inventory.getAmmoCount(mockAmmo));
    }

    @Test
    void testGetAmmoTypes_OnlyReturnsPositiveStock() {
        AmmoType mockAmmo1 = mock(AmmoType.class);
        AmmoType mockAmmo2 = mock(AmmoType.class);
        
        inventory.addAmmo(mockAmmo1, 5);
        inventory.addAmmo(mockAmmo2, 0); // Not actually added because addAmmo checks <= 0, but simulating 0 count
        
        List<AmmoType> types = inventory.getAmmoTypes();
        
        // Should contain the default HERound (from setup) and mockAmmo1, but not mockAmmo2
        assertTrue(types.stream().anyMatch(t -> t instanceof HERound));
        assertTrue(types.contains(mockAmmo1));
        assertFalse(types.contains(mockAmmo2));
    }

    @Test
    void testGetDefaultAmmoType_Found() {
        AmmoType mockHE = mock(AmmoType.class);
        when(mockHE.getName()).thenReturn("HE ROUND");
        inventory.addAmmo(mockHE, 10);
        
        AmmoType found = inventory.getDefaultAmmoType();
        
        assertNotNull(found);
        // Note: The built-in constructor adds a real HERound which also matches "HE Round".
        // This assertion simply validates that an ammo type matching the name is correctly returned.
        assertTrue("HE ROUND".equalsIgnoreCase(found.getName()));
    }

    @Test
    void testGetWeaponWithLargestExplosion() {
        AmmoType smallBomb = mock(AmmoType.class);
        AmmoType nuke = mock(AmmoType.class);
        
        when(smallBomb.getExplosionRadius()).thenReturn(10);
        when(nuke.getExplosionRadius()).thenReturn(100);
        
        inventory.addAmmo(smallBomb, 5);
        inventory.addAmmo(nuke, 2);
        
        AmmoType largest = inventory.getWeaponWithLargestExplosion();
        
        assertEquals(nuke, largest);
    }
}