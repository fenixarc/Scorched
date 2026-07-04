package scorched.sound;

public class MusicTracksList {

	// --- Space Battle (Main Menu) ---
		private static final double[] MAIN_BASS = { 110.0, 130.81, 164.81, 196.0, 164.81, 130.81 };
		private static final double[] MAIN_MELODY = { 440.00, 0.0, 440.00, 493.88, 523.25, 0.0, 587.33, 523.25, 493.88,
				440.00, 415.30, 440.00 };
		private static final int[] MAIN_DRUMS = { 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0 };
		
		private static final double[] MAIN_SYNTH = { 
				220.00, 220.00, 220.00, 220.00,
				261.63, 261.63, 261.63, 261.63,
				329.63, 329.63, 329.63, 329.63,
				392.00, 392.00, 392.00, 392.00,
				329.63, 329.63, 329.63, 329.63,
				293.66, 293.66, 293.66, 293.66
		};

		public static final MusicTrack MENU_THEME = new MusicTrack.Builder("Space Battle")
				.setNoteDurationMs(200)
				.setBass(MAIN_BASS, 0)
				.setMelody(MAIN_MELODY, 4)
				.setDrums(MAIN_DRUMS, 8)
				.setSynth(MAIN_SYNTH, 12)
				.build();
		
	// --- Desert Wasteland ---
	private static final double[] DESERT_BASS = { 73.42, 73.42, 77.78, 73.42, 87.31, 82.41 };
	private static final double[] DESERT_MELODY = { 293.66, 0.0, 311.13, 293.66, 349.23, 0.0, 329.63, 311.13, 293.66,
			0.0, 0.0, 0.0 };
	private static final int[] DESERT_DRUMS = { 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1 };
	
	public static final MusicTrack DESERT_WASTELAND = new MusicTrack.Builder("Desert Wasteland")
			.setNoteDurationMs(250)
			.setBass(DESERT_BASS, 0)
			.setMelody(DESERT_MELODY, 2)
			.setDrums(DESERT_DRUMS, 0)
			.setSynth(null, 0)
			.build();

	// --- Neon Citadel ---
	private static final double[] NEON_BASS = { 146.83, 164.81, 220.00, 196.00 };
	private static final double[] NEON_MELODY = { 587.33, 659.25, 880.00, 783.99, 880.00, 659.25, 587.33, 0.0 };
	private static final int[] NEON_DRUMS = { 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1 };
	
	public static final MusicTrack NEON_CITADEL = new MusicTrack.Builder("Neon Citadel")
			.setNoteDurationMs(140)
			.setBass(NEON_BASS, 0)
			.setMelody(NEON_MELODY, 3)
			.setDrums(NEON_DRUMS, 5)
			.setSynth(null, 0)
			.build();

	// --- Galactic Drop ---
	private static final double[] GALACTIC_BASS = { 87.31, 87.31, 87.31, 98.00, 110.00, 110.00, 110.00, 73.42 };
	private static final double[] GALACTIC_MELODY = { 349.23, 0.0, 349.23, 392.00, 440.00, 0.0, 440.00, 523.25, 493.88,
			440.00, 392.00, 349.23 };
	private static final int[] GALACTIC_DRUMS = { 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1 };
	
	public static final MusicTrack GALACTIC_DROP = new MusicTrack.Builder("Galactic Drop")
			.setNoteDurationMs(130)
			.setBass(GALACTIC_BASS, 0)
			.setMelody(GALACTIC_MELODY, 2)
			.setDrums(GALACTIC_DRUMS, 4)
			.setSynth(null, 0)
			.build();

	// --- Apex Predator ---
	private static final double[] APEX_BASS = { 55.00, 55.00, 65.41, 73.42, 55.00, 55.00, 87.31, 82.41 };
	private static final double[] APEX_MELODY = { 220.00, 220.00, 261.63, 293.66, 329.63, 0.0, 349.23, 329.63, 293.66,
			261.63, 220.00, 0.0 };
	private static final int[] APEX_DRUMS = { 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1 };
	
	public static final MusicTrack APEX_PREDATOR = new MusicTrack.Builder("Apex Predator")
			.setNoteDurationMs(110)
			.setBass(APEX_BASS, 0)
			.setMelody(APEX_MELODY, 1)
			.setDrums(APEX_DRUMS, 1)
			.setSynth(null, 0)
			.build();

	// --- Hell Diver ---
	private static final double[] HELL_BASS = { 87.31, 87.31, 73.42, 73.42, 65.41, 65.41, 98.00, 110.00 };
	private static final double[] HELL_MELODY = { 349.23, 0.0, 349.23, 392.00, 440.00, 0.0, 523.25, 493.88, 440.00, 0.0,
			392.00, 349.23, 293.66, 293.66, 0.0, 329.63, 349.23, 392.00, 440.00, 523.25, 587.33, 0.0, 523.25, 440.00 };
	private static final int[] HELL_DRUMS = { 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1 };
	
	public static final MusicTrack HELL_DIVER = new MusicTrack.Builder("Hell Diver")
			.setNoteDurationMs(125)
			.setBass(HELL_BASS, 0)
			.setMelody(HELL_MELODY, 4)
			.setDrums(HELL_DRUMS, 6)
			.setSynth(null, 0)
			.build();

	// --- Victory Fanfare (Game Over) ---
	private static final double[] VICTORY_BASS = { 130.81, 130.81, 146.83, 164.81, 174.61, 196.00, 196.00, 130.81 };
	private static final double[] VICTORY_MELODY = { 523.25, 0.0, 659.25, 0.0, 783.99, 659.25, 523.25, 0.0, 587.33,
			698.46, 783.99, 880.00, 987.77, 0.0, 1046.50, 0.0 };
	private static final int[] VICTORY_DRUMS = { 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1 };
	
	public static final MusicTrack VICTORY_THEME = new MusicTrack.Builder("Victory Fanfare")
			.setNoteDurationMs(120)
			.setBass(VICTORY_BASS, 0)
			.setMelody(VICTORY_MELODY, 0)
			.setDrums(VICTORY_DRUMS, 0)
			.setSynth(null, 0)
			.build();
}