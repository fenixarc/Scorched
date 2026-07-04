package scorched.sound;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

class SoundEngineTest {

    private MusicTrack testTrack;

    @BeforeEach
    void setUp() {
        // Cut off any running music threads from previous tests
        SoundEngine.stopMusic();

        // Properly assemble your MusicTrack instance using its real Builder pattern
        testTrack = new MusicTrack.Builder("Test Retro Beats")
                .setNoteDurationMs(10) // Keeping it short so tests execute rapidly
                .setBass(new double[]{55.0, 110.0}, 0)
                .setMelody(new double[]{440.0, 880.0}, 1)
                .setDrums(new int[]{1, 0, 1, 0}, 1)
                .setSynth(new double[]{220.0, 330.0}, 1)
                .build();
    }

    @AfterEach
    void tearDown() {
        // Guarantee clean up so background threads don't leak into subsequent tests
        SoundEngine.stopMusic();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testStartAndStopMusicLifecycle() throws InterruptedException {
        // Act: Start the engine with our valid MusicTrack instance
        SoundEngine.startMusic(testTrack);
        
        // Give the background sequencer thread a brief moment to spin up and process audio steps
        Thread.sleep(150);

        // Act & Assert: Stopping the music should join the thread and exit cleanly
        assertDoesNotThrow(() -> SoundEngine.stopMusic());
    }

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

    @Test
    void testNullPatternsInMusicTrack() {
        // Build an empty track via your real builder to confirm array-null safety fallbacks
        MusicTrack emptyTrack = new MusicTrack.Builder("Empty Track")
                .setNoteDurationMs(5)
                .setBass(null, 0)
                .setMelody(null, 0)
                .setDrums(null, 0)
                .setSynth(null, 0)
                .build();

        assertDoesNotThrow(() -> {
            SoundEngine.startMusic(emptyTrack);
            Thread.sleep(50);
            SoundEngine.stopMusic();
        });
    }
}