package com.scorched;

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

        // Instantiate your MusicTrack class directly using its constructor
        testTrack = new MusicTrack(
            "Test Retro Beats",                    // name
            new double[]{ 55.0, 110.0 },           // bassPattern
            new double[]{ 440.0, 880.0 },          // melodyPattern
            new int[]{ 1, 0, 1, 0 },               // drumsPattern
            10,                                    // noteDurationMs (short for fast tests)
            1,                                     // loopsBeforeMelody
            1                                      // loopsBeforeDrums
        );
    }

    @AfterEach
    void tearDown() {
        // Ensure threads are cleared out after execution
        SoundEngine.stopMusic();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testStartAndStopMusicLifecycle() throws InterruptedException {
        // Act: Start the engine with our valid MusicTrack instance
        SoundEngine.startMusic(testTrack);
        
        // Give the background sequencer thread a brief moment to spin up and open lines
        Thread.sleep(100);

        // Act & Assert: Stopping the music should join the thread and exit cleanly
        assertDoesNotThrow(() -> SoundEngine.stopMusic());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPlayFireSoundDoesNotThrow() {
        // Verifies the 150ms descending sweep math runs smoothly without array index crashes
        assertDoesNotThrow(() -> {
            SoundEngine.playFireSound();
            Thread.sleep(200);
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
    void testPlayTankDeathSoundDoesNotThrow() {
        // Verifies the heavy 800ms layered synthesis loop computes safely
        assertDoesNotThrow(() -> {
            SoundEngine.playTankDeathSound();
            Thread.sleep(850);
        });
    }

    @Test
    void testNullPatternsInMusicTrack() {
        // SoundEngine has null checks (e.g., bassPattern != null). 
        // This test passes null arrays to verify your engine's fallback logic.
        MusicTrack emptyTrack = new MusicTrack(
            "Empty Track", 
            null, 
            null, 
            null, 
            5, 
            0, 
            0
        );

        assertDoesNotThrow(() -> {
            SoundEngine.startMusic(emptyTrack);
            Thread.sleep(50);
            SoundEngine.stopMusic();
        });
    }
}