package scorched.utils;

import scorched.sound.MusicTrack;
import scorched.sound.MusicTracksList;
import scorched.sound.SoundEngine;

/**
 * A simple utility class to play music tracks via the SoundEngine.
 */
public class MusicPreview {

    private static MusicTrack currentTrack = MusicTracksList.HELL_DIVER;

    public static void main(String[] args) {
        if (currentTrack == null) {
            System.out.println("Cannot play a null track.");
            return;
        }
        
        SoundEngine.startMusic(currentTrack);
    }

}