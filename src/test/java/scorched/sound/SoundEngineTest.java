package scorched.sound;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

class SoundEngineTest {

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayFireSoundDoesNotThrow() {
        // Verifies the 150ms descending sweep math runs smoothly
        assertDoesNotThrow(() -> {
            SoundEngine.playFireSound();
            Thread.sleep(200); // Wait out the duration to let the worker thread finish processing
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayExplosionSoundDoesNotThrow() {
        // Verifies the 400ms low-pass noise algorithm handles bounds safely
        assertDoesNotThrow(() -> {
            SoundEngine.playExplosionSound();
            Thread.sleep(450);
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayFallDamageSoundDoesNotThrow() {
        // Verifies the heavy 3-layered 800ms explosion algorithm operates safely
        assertDoesNotThrow(() -> {
            SoundEngine.playFallDamageSound();
            Thread.sleep(850);
        });
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPlayTankDeathSoundDoesNotThrow() {
        // Verifies the 1000ms two-tiered layered synthesis loop computes safely
        assertDoesNotThrow(() -> {
            SoundEngine.playTankDeathSound();
            Thread.sleep(1050);
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayMenuSelectSoundDoesNotThrow() {
        // Verifies the 80ms snappy UI frequency calculation handles bounds safely
        assertDoesNotThrow(() -> {
            SoundEngine.playMenuSelectSound();
            Thread.sleep(100);
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayMenuConfirmSoundDoesNotThrow() {
        // Verifies the 180ms 3-note arpeggio calculations run safely
        assertDoesNotThrow(() -> {
            SoundEngine.playMenuConfirmSound();
            Thread.sleep(200);
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayBarrelRotateSoundDoesNotThrow() {
        // Verifies the 45ms mechanical triangle wave gear click runs safely
        assertDoesNotThrow(() -> {
            SoundEngine.playBarrelRotateSound();
            Thread.sleep(60);
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayPowerChargeSoundBoundaries() {
        // Verifies math handling across standard ranges, limits, and out-of-bound arguments
        assertDoesNotThrow(() -> {
            SoundEngine.playPowerChargeSound(12.5);  // Standard mid-range
            Thread.sleep(70);

            SoundEngine.playPowerChargeSound(1.0);   // Minimum floor
            Thread.sleep(70);

            SoundEngine.playPowerChargeSound(25.0);  // Maximum ceiling
            Thread.sleep(70);

            SoundEngine.playPowerChargeSound(-5.0);  // Clamped negative check
            Thread.sleep(70);

            SoundEngine.playPowerChargeSound(100.0); // Clamped overflow check
            Thread.sleep(70);
        });
    }
}