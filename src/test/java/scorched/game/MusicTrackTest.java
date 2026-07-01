package scorched.game;

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
    private final double[] expectedSynthPattern = {110.0, 165.0, 220.0};
    private final int expectedNoteDurationMs = 250;
    private final int expectedLoopsBeforeBass = 1;
    private final int expectedLoopsBeforeMelody = 2;
    private final int expectedLoopsBeforeDrums = 4;
    private final int expectedLoopsBeforeSynth = 3;

    @BeforeEach
    void setUp() {
        // Initialize a fresh instance using the Builder pattern before each test execution
        musicTrack = new MusicTrack.Builder(expectedName)
                .setNoteDurationMs(expectedNoteDurationMs)
                .setBass(expectedBassPattern, expectedLoopsBeforeBass)
                .setMelody(expectedMelodyPattern, expectedLoopsBeforeMelody)
                .setDrums(expectedDrumsPattern, expectedLoopsBeforeDrums)
                .setSynth(expectedSynthPattern, expectedLoopsBeforeSynth)
                .build();
    }

    @Test
    @DisplayName("Builder and Getters should correctly assign and return simple fields")
    void testSimpleFieldsAndGetters() {
        assertEquals(expectedName, musicTrack.getName(), "Name should match the builder input.");
        assertEquals(expectedNoteDurationMs, musicTrack.getNoteDurationMs(), "Note duration should match.");
        assertEquals(expectedLoopsBeforeBass, musicTrack.getLoopsBeforeBass(), "Loops before bass should match.");
        assertEquals(expectedLoopsBeforeMelody, musicTrack.getLoopsBeforeMelody(), "Loops before melody should match.");
        assertEquals(expectedLoopsBeforeDrums, musicTrack.getLoopsBeforeDrums(), "Loops before drums should match.");
        assertEquals(expectedLoopsBeforeSynth, musicTrack.getLoopsBeforeSynth(), "Loops before synth should match.");
    }

    @Test
    @DisplayName("Getters should return exact copies or references of the pattern arrays")
    void testPatternArrayGetters() {
        // Assert array equality (checks both length and element values)
        assertArrayEquals(expectedBassPattern, musicTrack.getBassPattern(), "Bass pattern array elements should match.");
        assertArrayEquals(expectedMelodyPattern, musicTrack.getMelodyPattern(), "Melody pattern array elements should match.");
        assertArrayEquals(expectedDrumsPattern, musicTrack.getDrumsPattern(), "Drums pattern array elements should match.");
        assertArrayEquals(expectedSynthPattern, musicTrack.getSynthPattern(), "Synth pattern array elements should match.");
    }

    @Test
    @DisplayName("Builder should supply correct default fallback parameters when optional methods are skipped")
    void testBuilderDefaults() {
        // Only provide the required name field
        MusicTrack defaultTrack = new MusicTrack.Builder("Default Track").build();

        assertEquals("Default Track", defaultTrack.getName());
        assertEquals(125, defaultTrack.getNoteDurationMs(), "Should fall back to default 125ms.");
        assertEquals(0, defaultTrack.getLoopsBeforeBass(), "Should default to 0 loops.");
        assertEquals(0, defaultTrack.getLoopsBeforeMelody(), "Should default to 0 loops.");
        assertEquals(0, defaultTrack.getLoopsBeforeDrums(), "Should default to 0 loops.");
        assertEquals(0, defaultTrack.getLoopsBeforeSynth(), "Should default to 0 loops.");
        assertNull(defaultTrack.getBassPattern(), "Unset patterns should default to null.");
    }

    @Test
    @DisplayName("Builder should handle empty patterns without throwing exceptions")
    void testEmptyPatterns() {
        MusicTrack emptyTrack = new MusicTrack.Builder("Silent Track")
                .setBass(new double[0], 0)
                .setMelody(new double[0], 0)
                .setDrums(new int[0], 0)
                .setSynth(new double[0], 0)
                .build();
        
        assertNotNull(emptyTrack.getBassPattern());
        assertEquals(0, emptyTrack.getBassPattern().length);
        
        assertNotNull(emptyTrack.getMelodyPattern());
        assertEquals(0, emptyTrack.getMelodyPattern().length);
        
        assertNotNull(emptyTrack.getDrumsPattern());
        assertEquals(0, emptyTrack.getDrumsPattern().length);

        assertNotNull(emptyTrack.getSynthPattern());
        assertEquals(0, emptyTrack.getSynthPattern().length);
    }

    @Test
    @DisplayName("Builder should handle explicit null pattern inputs gracefully")
    void testNullPatterns() {
        MusicTrack nullTrack = new MusicTrack.Builder("Null Track")
                .setBass(null, 0)
                .setMelody(null, 0)
                .setDrums(null, 0)
                .setSynth(null, 0)
                .build();
        
        assertNull(nullTrack.getBassPattern(), "Bass pattern should be null if initialized as null.");
        assertNull(nullTrack.getMelodyPattern(), "Melody pattern should be null if initialized as null.");
        assertNull(nullTrack.getDrumsPattern(), "Drums pattern should be null if initialized as null.");
        assertNull(nullTrack.getSynthPattern(), "Synth pattern should be null if initialized as null.");
    }
}