package scorched.sound;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MusicPlayerTest {

    private MockedStatic<AudioSystem> mockedAudioSystem;
    private SourceDataLine mockLine;
    private MusicTrack dummyTrack;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure state is clean before each test
        MusicPlayer.stopMusic();
        MusicPlayer.muteMusic.set(false);
        MusicPlayer.musicVolume.set(7);

        // Mock AudioSystem and SourceDataLine to prevent hardware dependency during unit tests
        mockedAudioSystem = mockStatic(AudioSystem.class);
        mockLine = mock(SourceDataLine.class);

        mockedAudioSystem.when(() -> AudioSystem.getSourceDataLine(any(AudioFormat.class)))
                .thenReturn(mockLine);

        // Create a minimal track mock
        dummyTrack = mock(MusicTrack.class);
        when(dummyTrack.getName()).thenReturn("Test Track");
        when(dummyTrack.getNoteDurationMs()).thenReturn(10); // Short step for fast tests
        when(dummyTrack.getBassPattern()).thenReturn(new double[]{440.0, 440.0});
        when(dummyTrack.getMelodyPattern()).thenReturn(new double[]{880.0});
        when(dummyTrack.getDrumsPattern()).thenReturn(new int[]{1});
        when(dummyTrack.getSynthPattern()).thenReturn(new double[]{220.0, 220.0});
        when(dummyTrack.getLoopsBeforeBass()).thenReturn(0);
        when(dummyTrack.getLoopsBeforeMelody()).thenReturn(0);
        when(dummyTrack.getLoopsBeforeDrums()).thenReturn(0);
        when(dummyTrack.getLoopsBeforeSynth()).thenReturn(0);
        when(dummyTrack.getBassVolumeModifier()).thenReturn(1.0);
    }

    @AfterEach
    void tearDown() {
        // Stop playback and close static mocks after every test
        MusicPlayer.stopMusic();
        if (mockedAudioSystem != null) {
            mockedAudioSystem.close();
        }
    }

    @Test
    @DisplayName("Initial volume and mute settings should match defaults")
    void testInitialSettings() {
        assertEquals(7, MusicPlayer.musicVolume.get());
        assertFalse(MusicPlayer.muteMusic.get());
    }
    
    @Test
    @DisplayName("Calling stopMusic when no track is playing should not throw exceptions")
    void testStopMusicWhenNotPlaying() {
        assertDoesNotThrow(MusicPlayer::stopMusic);
    }

    @Test
    @DisplayName("Calling startMusic while playing stops existing track and starts new track")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testRestartMusic() throws Exception {
        MusicTrack secondTrack = mock(MusicTrack.class);
        when(secondTrack.getName()).thenReturn("Second Track");
        when(secondTrack.getNoteDurationMs()).thenReturn(10);

        MusicPlayer.startMusic(dummyTrack);
        Thread.sleep(100);

        // Invoking startMusic again should trigger stopMusic() first
        MusicPlayer.startMusic(secondTrack);
        Thread.sleep(100);

        verify(dummyTrack, atLeastOnce()).getName();
        verify(secondTrack, atLeastOnce()).getName();
    }

    @Test
    @DisplayName("Handles track with null/empty patterns gracefully without throwing arithmetic exceptions")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testNullPatternsHandling() throws Exception {
        MusicTrack emptyTrack = mock(MusicTrack.class);
        when(emptyTrack.getName()).thenReturn("Empty Track");
        when(emptyTrack.getNoteDurationMs()).thenReturn(10);
        when(emptyTrack.getBassPattern()).thenReturn(null); // Triggers fallback STEPS_PER_LOOP = 1
        when(emptyTrack.getMelodyPattern()).thenReturn(new double[0]);
        when(emptyTrack.getDrumsPattern()).thenReturn(null);
        when(emptyTrack.getSynthPattern()).thenReturn(null);

        assertDoesNotThrow(() -> {
            MusicPlayer.startMusic(emptyTrack);
            Thread.sleep(100);
            MusicPlayer.stopMusic();
        });
    }

    @Test
    @DisplayName("Volume change and Mute flag modify audio output processing without error")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testVolumeAndMuteModifications() throws Exception {
        MusicPlayer.startMusic(dummyTrack);
        Thread.sleep(50);

        // Mute track
        MusicPlayer.muteMusic.set(true);
        Thread.sleep(50);

        // Adjust volume
        MusicPlayer.muteMusic.set(false);
        MusicPlayer.musicVolume.set(3);
        Thread.sleep(50);

        assertDoesNotThrow(MusicPlayer::stopMusic);
    }
}