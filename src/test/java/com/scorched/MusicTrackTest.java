package com.scorched;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MusicTrackTest {

    private MusicTrack musicTrack;
    
    // Test data
    private final String expectedName = "Cyberpunk Beats";
    private final double[] expectedBassPattern = {55.0, 55.0, 65.41, 73.42};
    private final double[] expectedMelodyPattern = {220.0, 440.0, 330.0, 0.0};
    private final int[] expectedDrumsPattern = {1, 0, 2, 0, 1, 1, 2, 0};
    private final int expectedNoteDurationMs = 250;
    private final int expectedLoopsBeforeMelody = 2;
    private final int expectedLoopsBeforeDrums = 4;

    @BeforeEach
    void setUp() {
        // Initialize a fresh instance before each test execution
        musicTrack = new MusicTrack(
                expectedName,
                expectedBassPattern,
                expectedMelodyPattern,
                expectedDrumsPattern,
                expectedNoteDurationMs,
                expectedLoopsBeforeMelody,
                expectedLoopsBeforeDrums
        );
    }

    @Test
    @DisplayName("Constructor and Getters should correctly assign and return simple fields")
    void testSimpleFieldsAndGetters() {
        assertEquals(expectedName, musicTrack.getName(), "Name should match the constructor input.");
        assertEquals(expectedNoteDurationMs, musicTrack.getNoteDurationMs(), "Note duration should match.");
        assertEquals(expectedLoopsBeforeMelody, musicTrack.getLoopsBeforeMelody(), "Loops before melody should match.");
        assertEquals(expectedLoopsBeforeDrums, musicTrack.getLoopsBeforeDrums(), "Loops before drums should match.");
    }

    @Test
    @DisplayName("Getters should return exact copies or references of the pattern arrays")
    void testPatternArrayGetters() {
        // Assert array equality (checks both length and element values)
        assertArrayEquals(expectedBassPattern, musicTrack.getBassPattern(), "Bass pattern array elements should match.");
        assertArrayEquals(expectedMelodyPattern, musicTrack.getMelodyPattern(), "Melody pattern array elements should match.");
        assertArrayEquals(expectedDrumsPattern, musicTrack.getDrumsPattern(), "Drums pattern array elements should match.");
    }

    @Test
    @DisplayName("Constructor should handle empty patterns without throwing exceptions")
    void testEmptyPatterns() {
        MusicTrack emptyTrack = new MusicTrack("Silent Track", new double[0], new double[0], new int[0], 120, 0, 0);
        
        assertNotNull(emptyTrack.getBassPattern());
        assertEquals(0, emptyTrack.getBassPattern().length);
        
        assertNotNull(emptyTrack.getMelodyPattern());
        assertEquals(0, emptyTrack.getMelodyPattern().length);
        
        assertNotNull(emptyTrack.getDrumsPattern());
        assertEquals(0, emptyTrack.getDrumsPattern().length);
    }

    @Test
    @DisplayName("Constructor should handle null pattern inputs gracefully")
    void testNullPatterns() {
        MusicTrack nullTrack = new MusicTrack("Null Track", null, null, null, 100, 0, 0);
        
        assertNull(nullTrack.getBassPattern(), "Bass pattern should be null if initialized as null.");
        assertNull(nullTrack.getMelodyPattern(), "Melody pattern should be null if initialized as null.");
        assertNull(nullTrack.getDrumsPattern(), "Drums pattern should be null if initialized as null.");
    }
}