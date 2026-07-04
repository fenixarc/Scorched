package scorched.sound;

public class MusicTrack {
	private final String name;
	private final int noteDurationMs;
	
	// Audio Patterns
	private final double[] bassPattern;
	private final double[] melodyPattern;
	private final int[] drumsPattern;
	private final double[] synthPattern;

	// Independent Loop Thresholds
	private final int loopsBeforeBass;
	private final int loopsBeforeMelody;
	private final int loopsBeforeDrums;
	private final int loopsBeforeSynth;
	
	/**
	 * Private Constructor - Only accessible via the Builder
	 */
	private MusicTrack(Builder builder) {
		this.name = builder.name;
		this.noteDurationMs = builder.noteDurationMs;
		this.bassPattern = builder.bassPattern;
		this.loopsBeforeBass = builder.loopsBeforeBass;
		this.melodyPattern = builder.melodyPattern;
		this.loopsBeforeMelody = builder.loopsBeforeMelody;
		this.drumsPattern = builder.drumsPattern;
		this.loopsBeforeDrums = builder.loopsBeforeDrums;
		this.synthPattern = builder.synthPattern;
		this.loopsBeforeSynth = builder.loopsBeforeSynth;
	}
	
	/**
	 * Static Inner Builder Class
	 */
	public static class Builder {
		private final String name;
		private int noteDurationMs = 125; // Default fallback value
		
		private double[] bassPattern;
		private int loopsBeforeBass = 0;
		
		private double[] melodyPattern;
		private int loopsBeforeMelody = 0;
		
		private int[] drumsPattern;
		private int loopsBeforeDrums = 0;
		
		private double[] synthPattern;
		private int loopsBeforeSynth = 0;

		// Required parameter goes in the Builder constructor
		public Builder(String name) {
			this.name = name;
		}

		public Builder setNoteDurationMs(int noteDurationMs) {
			this.noteDurationMs = noteDurationMs;
			return this; // Returns 'this' to allow method chaining
		}

		public Builder setBass(double[] pattern, int loopsBeforeStart) {
			this.bassPattern = pattern;
			this.loopsBeforeBass = loopsBeforeStart;
			return this;
		}

		public Builder setMelody(double[] pattern, int loopsBeforeStart) {
			this.melodyPattern = pattern;
			this.loopsBeforeMelody = loopsBeforeStart;
			return this;
		}

		public Builder setDrums(int[] pattern, int loopsBeforeStart) {
			this.drumsPattern = pattern;
			this.loopsBeforeDrums = loopsBeforeStart;
			return this;
		}

		public Builder setSynth(double[] pattern, int loopsBeforeStart) {
			this.synthPattern = pattern;
			this.loopsBeforeSynth = loopsBeforeStart;
			return this;
		}

		/**
		 * Compiles all configured values into the final immutable MusicTrack object
		 */
		public MusicTrack build() {
			return new MusicTrack(this);
		}
	}

	// --- Getters ---

	public String getName() {
		return name;
	}

	public int getNoteDurationMs() {
		return noteDurationMs;
	}

	public double[] getBassPattern() {
		return bassPattern;
	}

	public int getLoopsBeforeBass() {
		return loopsBeforeBass;
	}

	public double[] getMelodyPattern() {
		return melodyPattern;
	}

	public int getLoopsBeforeMelody() {
		return loopsBeforeMelody;
	}

	public int[] getDrumsPattern() {
		return drumsPattern;
	}

	public int getLoopsBeforeDrums() {
		return loopsBeforeDrums;
	}

	public double[] getSynthPattern() {
		return synthPattern;
	}

	public int getLoopsBeforeSynth() {
		return loopsBeforeSynth;
	}
}