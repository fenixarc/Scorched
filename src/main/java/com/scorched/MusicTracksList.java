package com.scorched;
public class MusicTracksList {

	// --- Space Battle (Main Menu) ---
	private static final double[] MAIN_BASS = { 110.0, 130.81, 164.81, 196.0, 164.81, 130.81 };
	private static final double[] MAIN_MELODY = { 440.00, 0.0, 440.00, 493.88, 523.25, 0.0, 587.33, 523.25, 493.88,
			440.00, 415.30, 440.00 };
	private static final int[] MAIN_DRUMS = { 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0 };
	public static final MusicTrack MENU_THEME = new MusicTrack("Space Battle", MAIN_BASS, MAIN_MELODY, MAIN_DRUMS, 200,
			4, 8);

	// --- Desert Wasteland ---
	private static final double[] DESERT_BASS = { 73.42, 73.42, 77.78, 73.42, 87.31, 82.41 };
	private static final double[] DESERT_MELODY = { 293.66, 0.0, 311.13, 293.66, 349.23, 0.0, 329.63, 311.13, 293.66,
			0.0, 0.0, 0.0 };
	private static final int[] DESERT_DRUMS = { 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1 };
	public static final MusicTrack DESERT_THEME = new MusicTrack("Desert Wasteland", DESERT_BASS, DESERT_MELODY,
			DESERT_DRUMS, 250, 2, 0);

	// --- Neon Citadel ---
	private static final double[] NEON_BASS = { 146.83, 164.81, 220.00, 196.00 };
	private static final double[] NEON_MELODY = { 587.33, 659.25, 880.00, 783.99, 880.00, 659.25, 587.33, 0.0 };
	private static final int[] NEON_DRUMS = { 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1 };
	public static final MusicTrack NEON_THEME = new MusicTrack("Neon Citadel", NEON_BASS, NEON_MELODY, NEON_DRUMS, 140,
			3, 5);

	// --- Victory Fanfare ---
	private static final double[] VICTORY_BASS = { 130.81, 130.81, 146.83, 164.81, 174.61, 196.00, 196.00, 130.81 };
	private static final double[] VICTORY_MELODY = { 523.25, 0.0, 659.25, 0.0, 783.99, 659.25, 523.25, 0.0, 587.33,
			698.46, 783.99, 880.00, 987.77, 0.0, 1046.50, 0.0 };
	private static final int[] VICTORY_DRUMS = { 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1 };
	public static final MusicTrack VICTORY_THEME = new MusicTrack("Victory Fanfare", VICTORY_BASS, VICTORY_MELODY,
			VICTORY_DRUMS, 120, 0, 0);
}