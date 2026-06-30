package com.scorched;
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
	 * engine dynamically. Timelines are driven by the bass line loop length,
	 * and the synth layer supports look-ahead envelope bridging for long, sustained notes.
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
			// Extract patterns from track
			double[] bassPattern = track.getBassPattern();
			double[] melodyPattern = track.getMelodyPattern();
			int[] drumsPattern = track.getDrumsPattern();
			double[] synthPattern = track.getSynthPattern();

			int samplesPerStep = (SAMPLE_RATE * track.getNoteDurationMs()) / 1000;
			Random rand = new Random();

			try {
				AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
				SourceDataLine line = AudioSystem.getSourceDataLine(format);
				line.open(format, 1024);
				line.start();

				// Universal master step counter (64-bit to prevent long playback overflow)
				long totalSteps = 0; 

				// Dynamic Master Clock: Defined by the actual size of the bass line array.
				// Fallback to 1 to prevent division-by-zero crashes if a track has no bass.
				final int STEPS_PER_LOOP = (bassPattern != null && bassPattern.length > 0) ? bassPattern.length : 1;

				while (playMusic) {
					// --- 1. CALCULATE CURRENT LOOP TIMELINE ---
					// Tracks exactly how many times the bass line has fully completed
					long currentGlobalLoop = totalSteps / STEPS_PER_LOOP;

					// --- 2. EVALUATE LAYER ACTIVATION AGAINST BASS CLOCK ---
					boolean bassActive = (bassPattern != null && bassPattern.length > 0) 
							&& (currentGlobalLoop >= track.getLoopsBeforeBass());

					boolean melodyActive = (melodyPattern != null && melodyPattern.length > 0) 
							&& (currentGlobalLoop >= track.getLoopsBeforeMelody());

					boolean drumsActive = (drumsPattern != null && drumsPattern.length > 0) 
							&& (currentGlobalLoop >= track.getLoopsBeforeDrums());

					boolean synthActive = (synthPattern != null && synthPattern.length > 0) 
							&& (currentGlobalLoop >= track.getLoopsBeforeSynth());

					// --- 3. FETCH PATTERN INDICES AND LOOK-AHEAD LOGIC ---
					int bassIndex   = (bassActive)   ? (int)(totalSteps % bassPattern.length)   : 0;
					int melodyIndex = (melodyActive) ? (int)(totalSteps % melodyPattern.length) : 0;
					int drumIndex   = (drumsActive)  ? (int)(totalSteps % drumsPattern.length)  : 0;
					
					int synthIndex  = (synthActive)  ? (int)(totalSteps % synthPattern.length)  : 0;
					
					// Look ahead exactly 1 step into the synth array to check for tied notes
					int nextSynthIndex = (synthActive) ? (int)((totalSteps + 1) % synthPattern.length) : 0;

					double bassFreq   = (bassActive)   ? bassPattern[bassIndex]   : 0;
					double melodyFreq = (melodyActive) ? melodyPattern[melodyIndex] : 0;
					int drumHit       = (drumsActive)  ? drumsPattern[drumIndex]  : 0;
					
					double synthFreq     = (synthActive) ? synthPattern[synthIndex] : 0;
					double nextSynthFreq = (synthActive) ? synthPattern[nextSynthIndex] : 0;

					// Determine if the current synth note continues seamlessly into the next step
					boolean synthNoteIsTied = (synthFreq > 0.0 && synthFreq == nextSynthFreq);

					byte[] buffer = new byte[samplesPerStep];

					// --- 4. AUDIO SIGNAL GENERATION ---
					for (int i = 0; i < samplesPerStep; i++) {
						double totalSignal = 0.0;
						double stepProgress = (double) i / samplesPerStep;
						double volumeEnvelope = 1.0 - stepProgress;

						// CHANNEL 1: Bassline (Sine Wave)
						if (bassActive && bassFreq > 0.0) {
							double bassAngle = 2.0 * Math.PI * bassFreq * i / SAMPLE_RATE;
							totalSignal += Math.sin(bassAngle) * 22 * volumeEnvelope;
						}

						// CHANNEL 2: Melody Lead (Square Wave)
						if (melodyActive && melodyFreq > 0.0) {
							double melodyAngle = 2.0 * Math.PI * melodyFreq * i / SAMPLE_RATE;
							double melodyWave = (Math.sin(melodyAngle) >= 0.0) ? 1.0 : -1.0;
							totalSignal += melodyWave * 12 * Math.pow(1.0 - stepProgress, 2);
						}

						// CHANNEL 3: Dynamic Drums Layer (White Noise Burst)
						if (drumsActive && drumHit == 1) {
							double whiteNoise = (rand.nextDouble() * 2.0) - 1.0;
							double drumEnvelope = Math.pow(1.0 - stepProgress, 5);
							totalSignal += whiteNoise * 20 * drumEnvelope;
						}

						// CHANNEL 4: Synth Pad (Triangle Wave with Bridged Envelope)
						if (synthActive && synthFreq > 0.0) {
							double synthAngle = 2.0 * Math.PI * synthFreq * i / SAMPLE_RATE;
							double triangleWave = (2.0 / Math.PI) * Math.asin(Math.sin(synthAngle));
							
							double synthEnvelope;
							if (synthNoteIsTied) {
								// Keep volume near full capacity to bridge into the next step smoothly
								synthEnvelope = 1.0 - (stepProgress * 0.15); 
							} else {
								// Final step of the note block: allow standard decay to finish cleanly
								synthEnvelope = 1.0 - stepProgress;
							}
							
							totalSignal += triangleWave * 15 * synthEnvelope;
						}

						// Protect audio driver from overflow clipping bounds
						if (totalSignal > 127)  totalSignal = 127;
						if (totalSignal < -128) totalSignal = -128;

						buffer[i] = (byte) totalSignal;
					}

					line.write(buffer, 0, buffer.length);
					totalSteps++;
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
	public static void playFallDamageSound() {
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
	
	/**
	 * Synthesizes a crisp, short 8-bit blip mimicking a menu item selection.
	 * Uses a rapid frequency jump and sharp exponential decay for responsiveness.
	 */
	public static void playMenuSelectSound() {
		int durationMs = 80; // Short and snappy
		int numSamples = (SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Frequency starts clear at 600Hz and leaps up to 1200Hz halfway through
			double frequency = (progress < 0.4) ? 600.0 : 1200.0;
			double angle = 2.0 * Math.PI * frequency * i / SAMPLE_RATE;

			// Blending a square wave (for retro crunch) and sine wave (for clarity)
			double squareWave = (Math.sin(angle) >= 0.0) ? 1.0 : -1.0;
			double sineWave = Math.sin(angle);
			double mixedWave = (sineWave * 0.4) + (squareWave * 0.6);

			// Sharp exponential decay envelope so it doesn't linger
			double volumeEnvelope = Math.pow(1.0 - progress, 3);
			
			// Map to 8-bit byte bounds (max amplitude around 60 to keep it pleasant)
			buffer[i] = (byte) (mixedWave * 60.0 * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a bright, rapid 3-note arpeggio mimicking a menu confirmation.
	 * Progresses quickly through a major triad for a rewarding, positive UI feel.
	 */
	public static void playMenuConfirmSound() {
		int durationMs = 180; // Slightly longer to allow the chord to resolve
		int numSamples = (SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Divide the sound into 3 rapid stages (an 8-bit arpeggio)
			double frequency;
			if (progress < 0.25) {
				frequency = 523.25; // Note C5
			} else if (progress < 0.50) {
				frequency = 659.25; // Note E5
			} else {
				frequency = 783.99; // Note G5
			}

			double angle = 2.0 * Math.PI * frequency * i / SAMPLE_RATE;

			// Duty cycle modulation for a classic "chiptune" pulse width flavor
			// Alternates between a square wave and a lean pulse wave over time
			double dutyCycle = 0.5 - (progress * 0.25); 
			double pulseWave = (Math.sin(angle) >= Math.sin(dutyCycle * Math.PI)) ? 1.0 : -1.0;

			// Smooth exponential decay envelope so the final note fades out cleanly
			double volumeEnvelope = Math.pow(1.0 - progress, 2);
			
			// Map to 8-bit byte bounds (kept at a balanced volume level)
			buffer[i] = (byte) (pulseWave * 65.0 * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a rapid, mechanical gear click for rotating the tank barrel.
	 * Intended to be triggered repeatedly in the game loop while the barrel turns.
	 */
	public static void playBarrelRotateSound() {
		int durationMs = 45; // Extremely short for rapid, gapless repetition
		int numSamples = (SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random rand = new Random();

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Low-frequency mechanical hum (90Hz) shifting down slightly
			double frequency = 90.0 - (progress * 20.0);
			double angle = 2.0 * Math.PI * frequency * i / SAMPLE_RATE;

			// Generate a harsh triangle/sawtooth hybrid for gear tooth friction
			double triangleWave = (Math.abs((angle % (2.0 * Math.PI)) - Math.PI) / Math.PI) * 2.0 - 1.0;
			
			// Inject a tiny burst of metal-on-metal friction noise at the start of the click
			double mechanicalNoise = (rand.nextDouble() * 2.0 - 1.0) * (1.0 - progress);
			
			double mixedSignal = (triangleWave * 0.7) + (mechanicalNoise * 0.3);

			// Linear fade out so consecutive gear clicks blend seamlessly
			double volumeEnvelope = 1.0 - progress;
			
			double volumeScale = 12.0;
			
			// Kept relatively quiet so a continuous loop isn't deafening
			buffer[i] = (byte) (mixedSignal * volumeScale * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a charging energy pulse for when the tank is raising power.
	 * Call this repeatedly in the game loop while charging, passing the current power level (1 to 25).
	 */
	public static void playPowerChargeSound(double powerLevel) {
		// Normalize the 1-25 range into a 0.0 to 1.0 ratio for the synth math
		double powerRatio = (powerLevel - 1) / 24.0;
		
		// Bound safety check
		if (powerRatio < 0.0) powerRatio = 0.0;
		if (powerRatio > 1.0) powerRatio = 1.0;

		int durationMs = 60; // Short window for smooth, continuous updates
		int numSamples = (SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random rand = new Random();

		// Base frequency sweeps upward exponentially based on the power ratio
		// Starts at a low hum (120Hz) and climbs to a high-energy whine (880Hz)
		double startFreq = 120.0 + (Math.pow(powerRatio, 2) * 760.0);
		// Add a slight pitch upward within the individual pulse itself for motion
		double endFreq = startFreq + 30.0;

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			double currentFreq = startFreq * (1.0 - progress) + (endFreq * progress);
			
			// Track phase angle
			double angle = 2.0 * Math.PI * currentFreq * i / SAMPLE_RATE;

			// Channel 1: Core energy wave (Sine wave for fundamental tone)
			double coreSignal = Math.sin(angle);

			// Channel 2: Heavy charging texture (Square wave modulated by power level)
			double dutyCycle = 0.5 - (powerRatio * 0.3);
			double pulseSignal = (Math.sin(angle) >= Math.sin(dutyCycle * Math.PI)) ? 1.0 : -1.0;

			// Channel 3: Unstable plasma hum (White noise injected more heavily at max power)
			double instabilityNoise = (rand.nextDouble() * 2.0 - 1.0) * powerRatio * 0.4;

			// Blend layers dynamically
			double mixedSignal = (coreSignal * 0.5) + (pulseSignal * (0.2 + powerRatio * 0.3)) + instabilityNoise;

			// Smooth volume envelope to prevent clicking between updates
			double volumeEnvelope = Math.sin(progress * Math.PI); 
			
			// LOWERED VOLUME SCALE: Sits between 10.0 and 20.0 (Down from 25.0 - 50.0)
			// This keeps it background-level compared to explosions and gunfire.
			double volumeScale = 8.0 + (powerRatio * 8.0);

			buffer[i] = (byte) (mixedSignal * volumeScale * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a two-tiered explosion blast lasting approximately 1 second.
	 * Features a louder second blast and randomized pitches for both tiers 
	 * to ensure distinct sound variations on each playback.
	 */
	public static void playTankDeathSound() {
		int totalDurationMs = 1000;
		int numSamples = (SAMPLE_RATE * totalDurationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random random = new Random();

		// Timing offsets in samples
		int tier1Duration = (SAMPLE_RATE * 700) / 1000;  // First blast lasts 700ms
		int tier2Start = (SAMPLE_RATE * 300) / 1000;     // Second blast starts at 300ms
		int tier2Duration = numSamples - tier2Start;      // Second blast lasts remaining 700ms

		// Pitch Randomization: Slightly alters the low-pass filter coefficients each run
		// Tier 1 base filter center is ~0.92 (deeper). Variance: 0.90 to 0.94
		double tier1FilterBase = 0.90 + (random.nextDouble() * 0.04); 
		double tier1FilterInv = 1.0 - tier1FilterBase;

		// Tier 2 base filter center is ~0.74 (higher/sharper). Variance: 0.70 to 0.78
		double tier2FilterBase = 0.70 + (random.nextDouble() * 0.08);
		double tier2FilterInv = 1.0 - tier2FilterBase;

		double lowPassTier1 = 0.0;
		double lowPassTier2 = 0.0;

		for (int i = 0; i < numSamples; i++) {
			double tier1Sample = 0.0;
			double tier2Sample = 0.0;
			double rawNoise = random.nextInt(256) - 128;

			// --- Tier 1: Low-pitched initial rumble ---
			if (i < tier1Duration) {
				double progress1 = (double) i / tier1Duration;
				lowPassTier1 = lowPassTier1 * tier1FilterBase + rawNoise * tier1FilterInv;
				double envelope1 = 1.0 - progress1;
				tier1Sample = lowPassTier1 * envelope1;
			}

			// --- Tier 2: Higher-pitched overlapping blast (Louder) ---
			if (i >= tier2Start) {
				int tier2Index = i - tier2Start;
				double progress2 = (double) tier2Index / tier2Duration;
				lowPassTier2 = lowPassTier2 * tier2FilterBase + rawNoise * tier2FilterInv;
				double envelope2 = 1.0 - progress2; 
				tier2Sample = lowPassTier2 * envelope2;
			}

			// --- Combine and Mix ---
			// Tier 1 volume is scaled back (0.5) while Tier 2 is pushed forward (1.1) for maximum impact
			double mixedSample = (tier1Sample * 0.5) + (tier2Sample * 1.1);

			// Hard clamp to prevent digital distortion clipping past byte boundaries
			if (mixedSample > 127) mixedSample = 127;
			if (mixedSample < -128) mixedSample = -128;

			buffer[i] = (byte) mixedSample;
		}

		playGeneratedSound(buffer);
	}
}