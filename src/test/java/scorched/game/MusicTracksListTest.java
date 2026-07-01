package scorched.game;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class MusicTracksListTest {

    @Test
    public void testMenuThemeInitialization() {
        MusicTrack track = MusicTracksList.MENU_THEME;
        
        assertNotNull(track, "MENU_THEME should not be null");
        assertEquals("Space Battle", track.getName());
        assertEquals(200, track.getNoteDurationMs());
        assertEquals(4, track.getLoopsBeforeMelody());
        assertEquals(8, track.getLoopsBeforeDrums());
        
        // Assert loop delay for the new synth channel layer
        assertTrue(track.getLoopsBeforeSynth() >= 0, "Loops before synth should be initialized.");
        
        // Match specific array lengths and values
        double[] expectedBass = { 110.0, 130.81, 164.81, 196.0, 164.81, 130.81 };
        assertArrayEquals(expectedBass, track.getBassPattern(), 0.001);
        
        int[] expectedDrums = { 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0 };
        assertArrayEquals(expectedDrums, track.getDrumsPattern());
    }

    @Test
    public void testDesertThemeInitialization() {
        MusicTrack track = MusicTracksList.DESERT_WASTELAND;
        
        assertNotNull(track, "DESERT_THEME should not be null");
        assertEquals("Desert Wasteland", track.getName());
        assertEquals(250, track.getNoteDurationMs());
        assertEquals(2, track.getLoopsBeforeMelody());
        assertEquals(0, track.getLoopsBeforeDrums());
        
        // Assert loop delay for the new synth channel layer
        assertTrue(track.getLoopsBeforeSynth() >= 0, "Loops before synth should be initialized.");
        
        double[] expectedMelody = { 293.66, 0.0, 311.13, 293.66, 349.23, 0.0, 329.63, 311.13, 293.66, 0.0, 0.0, 0.0 };
        assertArrayEquals(expectedMelody, track.getMelodyPattern(), 0.001);
    }

    @Test
    public void testNeonThemeInitialization() {
        MusicTrack track = MusicTracksList.NEON_CITADEL;
        
        assertNotNull(track, "NEON_THEME should not be null");
        assertEquals("Neon Citadel", track.getName());
        assertEquals(140, track.getNoteDurationMs());
        assertEquals(3, track.getLoopsBeforeMelody());
        assertEquals(5, track.getLoopsBeforeDrums());
        
        // Assert loop delay for the new synth channel layer
        assertTrue(track.getLoopsBeforeSynth() >= 0, "Loops before synth should be initialized.");
        
        double[] expectedBass = { 146.83, 164.81, 220.00, 196.00 };
        assertArrayEquals(expectedBass, track.getBassPattern(), 0.001);
        
        // Tip: If your Neon theme uses the new synth pad channel, add your array check here:
        // double[] expectedSynth = { ... };
        // assertArrayEquals(expectedSynth, track.getSynthPattern(), 0.001);
    }

    @Test
    public void testVictoryThemeInitialization() {
        MusicTrack track = MusicTracksList.VICTORY_THEME;
        
        assertNotNull(track, "VICTORY_THEME should not be null");
        assertEquals("Victory Fanfare", track.getName());
        assertEquals(120, track.getNoteDurationMs());
        assertEquals(0, track.getLoopsBeforeMelody());
        assertEquals(0, track.getLoopsBeforeDrums());
        
        // Assert loop delay for the new synth channel layer
        assertEquals(0, track.getLoopsBeforeSynth(), "Victory track should trigger synth immediately if configured.");
        
        int[] expectedDrums = { 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1 };
        assertArrayEquals(expectedDrums, track.getDrumsPattern());
    }
}