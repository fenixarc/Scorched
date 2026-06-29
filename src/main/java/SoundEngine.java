import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;

public class SoundEngine {

	private static final int SAMPLE_RATE = 16000; // 16kHz audio quality

	// Volatile flag allows different threads to safely communicate when to stop the
	// music
	private static volatile boolean playMusic = false;
	private static Thread musicThread;

	/**
	 * Plays a sound effect asynchronously so it doesn't freeze the main game loop
	 * thread.
	 */
	private static void playGeneratedSound(byte[] buffer) {
		new Thread(() -> {
			try {
				// 16,000 samples per second, 8-bit mono
				AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
				SourceDataLine line = AudioSystem.getSourceDataLine(format);

				// Force Java to dump the sound to speakers immediately
				int tinyBufferSize = 256;
				line.open(format, tinyBufferSize);

				line.start();
				line.write(buffer, 0, buffer.length);
				line.drain();
				line.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Accepts ANY Track object configuration and streams it through the synth
	 * engine dynamically.
	 */
	public static void startMusic(MusicTrack track) {
		// Check if music needs to stop
		if (playMusic) {
			stopMusic();
		}

		// If already playing music, ignore
		if (playMusic)
			return;

		playMusic = true;

		System.out.println("Now Playing: " + track.getName());

		musicThread = new Thread(() -> {
			double[] bassPattern = track.getBassPattern();
			double[] melodyPattern = track.getMelodyPattern();
			int[] drumsPattern = track.getDrumsPattern();

			int samplesPerStep = (SAMPLE_RATE * track.getNoteDurationMs()) / 1000;
			Random rand = new Random();

			try {
				AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
				SourceDataLine line = AudioSystem.getSourceDataLine(format);
				line.open(format, 1024);
				line.start();

				int stepTracker = 0;
				int bassCyclesCompleted = 0;

				boolean melodyActive = false;
				boolean drumsActive = false;

				while (playMusic) {
					// Safety check array sizing lengths dynamically
					int bassIndex = (bassPattern != null) ? stepTracker % bassPattern.length : 0;
					int melodyIndex = (melodyPattern != null) ? stepTracker % melodyPattern.length : 0;
					int drumIndex = (drumsPattern != null) ? stepTracker % drumsPattern.length : 0;

					double bassFreq = (bassPattern != null) ? bassPattern[bassIndex] : 0;
					double melodyFreq = (melodyPattern != null) ? melodyPattern[melodyIndex] : 0;
					int drumHit = (drumsPattern != null) ? drumsPattern[drumIndex] : 0;

					// --- TRACK CYCLE PROGRESSION TIMELINE ---
					if (stepTracker > 0 && bassPattern != null && stepTracker % bassPattern.length == 0) {
						bassCyclesCompleted++;

						// Check Melody Unlock Threshold
						if (bassCyclesCompleted >= track.getLoopsBeforeMelody()) {
							melodyActive = true;
						}
						// Check Drum Unlock Threshold
						if (bassCyclesCompleted >= track.getLoopsBeforeDrums()) {
							drumsActive = true;
						}
					}

					byte[] buffer = new byte[samplesPerStep];

					for (int i = 0; i < samplesPerStep; i++) {
						double totalSignal = 0.0;
						double stepProgress = (double) i / samplesPerStep;
						double volumeEnvelope = 1.0 - stepProgress;

						// CHANNEL 1: The Bassline (Sine Wave)
						if (bassFreq > 0.0) {
							double bassAngle = 2.0 * Math.PI * bassFreq * i / SAMPLE_RATE;
							totalSignal += Math.sin(bassAngle) * 22 * volumeEnvelope;
						}

						// CHANNEL 2: The Melody Lead (Square Wave)
						if (melodyActive && melodyFreq > 0.0) {
							double melodyAngle = 2.0 * Math.PI * melodyFreq * i / SAMPLE_RATE;
							double melodyWave = (Math.sin(melodyAngle) >= 0.0) ? 1.0 : -1.0;
							totalSignal += melodyWave * 12 * Math.pow(1.0 - stepProgress, 2);
						}

						// CHANNEL 3: The Dynamic Drums Layer (White Noise Burst)
						if (drumsActive && drumHit == 1) {
							// Generate raw high-frequency white noise (-1.0 to 1.0)
							double whiteNoise = (rand.nextDouble() * 2.0) - 1.0;

							// Give drums an incredibly steep volume drop-off envelope
							// so it sounds like a sharp crisp tap instead of a long explosion hum
							double drumEnvelope = Math.pow(1.0 - stepProgress, 5);

							totalSignal += whiteNoise * 20 * drumEnvelope;
						}

						// Protect audio driver from overflow clipping bounds
						if (totalSignal > 127)
							totalSignal = 127;
						if (totalSignal < -128)
							totalSignal = -128;

						buffer[i] = (byte) totalSignal;
					}

					line.write(buffer, 0, buffer.length);
					stepTracker++;
				}

				line.drain();
				line.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		musicThread.start();
	}

	/**
	 * Instantly cuts off the music thread.
	 */
	public static void stopMusic() {
		// If music isn't playing, ignore
		if (!playMusic || musicThread == null)
			return;

		// Tell music to stop
		playMusic = false;

		try {
			// Force new song to wait until old song stops
			musicThread.join();
			System.out.println("Stopping track.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		musicThread = null;
	}

	/**
	 * Synthesizes a rapid descending frequency sweep mimicking a cannon blast.
	 */
	public static void playFireSound() {
		int durationMs = 150;
		int numSamples = (SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			// Frequency slides rapidly down from 400Hz to 60Hz
			double frequency = 400.0 * (1.0 - progress) + 60.0;
			double angle = 2.0 * Math.PI * frequency * i / SAMPLE_RATE;

			// Generate a square/sine blend wave and apply a volume decay envelope
			double volumeEnvelope = 1.0 - progress;
			buffer[i] = (byte) (Math.sin(angle) * 127.0 * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}

	/**
	 * Synthesizes a low-frequency white noise rumble mimicking a heavy explosion.
	 */
	public static void playExplosionSound() {
		int durationMs = 400;
		int numSamples = (SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random random = new Random();

		double lowPassFilter = 0.0;

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;

			// Generate raw white noise (-127 to 127)
			double noise = random.nextInt(256) - 128;

			// Apply a basic low-pass filter to make the noise deep and rumbling
			lowPassFilter = lowPassFilter * 0.85 + noise * 0.15;

			// Volume drop-off over the lifespan of the explosion
			double volumeEnvelope = 1.0 - progress;

			buffer[i] = (byte) (lowPassFilter * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Streams an incredibly heavy, multi-layered 8-bit explosion sound effect.
	 * Simulates an initial supersonic shockwave, shattering metal, and a deep secondary rumble.
	 */
	public static void playTankDeathSound() {
	    new Thread(() -> {
	        try {
	            // Extended to 800ms to let the massive sub-bass rumble decay naturally
	            int durationMs = 800;
	            int totalSamples = (SAMPLE_RATE * durationMs) / 1000;
	            byte[] buffer = new byte[totalSamples];
	            java.util.Random rand = new java.util.Random();

	            // We track an internal phase angle for our low-frequency sound generators
	            double rumblePhase = 0.0;

	            for (int i = 0; i < totalSamples; i++) {
	                double progress = (double) i / totalSamples;
	                double msElapsed = (double) i / SAMPLE_RATE * 1000.0;

	                double signal = 0.0;

	                // LAYER 1: The Supersonic Shockwave (First 60 milliseconds)
	                if (msElapsed < 60) {
	                    // Maximum pressure white noise with zero decay to clip the audio line aggressively
	                    signal += (rand.nextDouble() * 2.0 - 1.0) * 1.2;
	                }

	                // LAYER 2: Tearing Metal & Fire (First 250 milliseconds)
	                if (msElapsed < 250) {
	                    double noise = rand.nextDouble() * 2.0 - 1.0;
	                    
	                    // High-frequency square-ish crunch representing structural hull failure
	                    double metalCrack = (Math.sin(i * 0.4) >= 0.0) ? 0.4 : -0.4;
	                    
	                    // Rapid volume fade out just for this metallic layer
	                    double metalEnvelope = Math.pow(1.0 - (msElapsed / 250.0), 2);
	                    
	                    signal += (noise * 0.6 + metalCrack * 0.4) * metalEnvelope;
	                }

	                // LAYER 3: The Expanding Fuel Cook-Off & Deep Sub-Bass Rumble (Whole Duration)
	                // The frequency rapidly drops over time: starts at 90Hz and plunges down to an ultra-low 25Hz rumble
	                double currentFreq = 90.0 * Math.pow(1.0 - progress, 3) + 25.0;
	                rumblePhase += (2.0 * Math.PI * currentFreq) / SAMPLE_RATE;
	                
	                // Pure sine wave for that deep, chest-hitting sub-bass structure
	                double subBass = Math.sin(rumblePhase);
	                
	                // Modulate the sub-bass with white noise to make it sound dirty and explosive
	                double dirtyRumble = subBass * 0.5 + (subBass * (rand.nextDouble() * 2.0 - 1.0) * 0.5);
	                
	                // Long, smooth exponential decay envelope for the final rumble trail
	                double rumbleEnvelope = Math.pow(1.0 - progress, 4);
	                
	                signal += dirtyRumble * 1.0 * rumbleEnvelope;

	                // --- MASTER SATURATION & DRIVE BLOCK ---
	                // Amplify the mixed layers significantly to force digital distortion/overdrive
	                double highGainSignal = signal * 75.0;

	                // Hard clipping limits to protect the hardware buffer and create a gritty 8-bit distortion punch
	                if (highGainSignal > 127) highGainSignal = 127;
	                if (highGainSignal < -128) highGainSignal = -128;

	                buffer[i] = (byte) highGainSignal;
	            }

	            // Stream the high-intensity data array to the system audio hardware
	            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
	            SourceDataLine line = AudioSystem.getSourceDataLine(format);
	            line.open(format, 1024);
	            line.start();
	            line.write(buffer, 0, buffer.length);
	            line.drain();
	            line.close();

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }).start();
	}
}