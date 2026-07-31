package scorched.sound;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import scorched.game.Tank;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SoundEngine {
	
	// Audio override variables controlled by the settings menu
	public static final AtomicInteger soundVolume = new AtomicInteger(7);     // Scale from 1 to 10
	public static final AtomicBoolean muteSound = new AtomicBoolean(false);

	// Thread management
    private static final ExecutorService sfxExecutor = Executors.newFixedThreadPool(4);

	/**
	 * Plays a sound effect asynchronously so it doesn't freeze the main game loop
	 * thread.
	 */
	private static void playGeneratedSound(byte[] buffer) {
		if (muteSound.get()) return; // Sound override check
		sfxExecutor.submit(() -> {
			try {
				// 16,000 samples per second, 8-bit mono
				SourceDataLine line = AudioSystem.getSourceDataLine(AudioUtils.AUDIO_FORMAT);

				// Force Java to dump the sound to speakers immediately
				line.open(AudioUtils.AUDIO_FORMAT, AudioUtils.BUFFER_SIZE);

				line.start();

				// Apply volume scaling override to the buffer
				byte[] scaledBuffer = new byte[buffer.length];
				double volumeScale = soundVolume.get() / 10.0;
				for (int i = 0; i < buffer.length; i++) {
					scaledBuffer[i] = (byte) (buffer[i] * volumeScale);
				}
				
				line.write(scaledBuffer, 0, scaledBuffer.length);
				line.drain();
				line.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Synthesizes a rapid descending frequency sweep mimicking a cannon blast.
	 */
	public static void playFireSound() {
		int durationMs = 150;
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			// Frequency slides rapidly down from 400Hz to 60Hz
			double frequency = 400.0 * (1.0 - progress) + 60.0;
			double angle = 2.0 * Math.PI * frequency * i / AudioUtils.SAMPLE_RATE;

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
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
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
		// Extended to 800ms to let the massive sub-bass rumble decay naturally
		int durationMs = 800;
		int totalSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[totalSamples];
		java.util.Random rand = new java.util.Random();

		// We track an internal phase angle for our low-frequency sound generators
		double rumblePhase = 0.0;

		for (int i = 0; i < totalSamples; i++) {
			double progress = (double) i / totalSamples;
			double msElapsed = (double) i / AudioUtils.SAMPLE_RATE * 1000.0;

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
			rumblePhase += (2.0 * Math.PI * currentFreq) / AudioUtils.SAMPLE_RATE;
			
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
			highGainSignal = AudioUtils.clampToByte(highGainSignal);

			buffer[i] = (byte) highGainSignal;
		}

		// Pass the synthesized buffer off to the central asynchronous sound engine player
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a crisp, short 8-bit blip mimicking a menu item selection.
	 * Uses a rapid frequency jump and sharp exponential decay for responsiveness.
	 */
	public static void playMenuSelectSound() {
		int durationMs = 80; // Short and snappy
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Frequency starts clear at 600Hz and leaps up to 1200Hz halfway through
			double frequency = (progress < 0.4) ? 600.0 : 1200.0;
			double angle = 2.0 * Math.PI * frequency * i / AudioUtils.SAMPLE_RATE;

			// Blending a square wave (for retro crunch) and sine wave (for clarity)
			double squareWave = squareWave(angle);
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
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
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

			double angle = 2.0 * Math.PI * frequency * i / AudioUtils.SAMPLE_RATE;

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
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random rand = new Random();

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Low-frequency mechanical hum (90Hz) shifting down slightly
			double frequency = 90.0 - (progress * 20.0);
			double angle = 2.0 * Math.PI * frequency * i / AudioUtils.SAMPLE_RATE;

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
	 * Call this repeatedly in the game loop while charging, passing the current power level.
	 */
	public static void playPowerChargeSound(double powerLevel) {
		// Normalize the range into a 0.0 to 1.0 ratio for the synth math
		double powerRatio = (powerLevel - 1) / Tank.getMaxPower();
		
		// Bound safety check
		if (powerRatio < 0.0) powerRatio = 0.0;
		if (powerRatio > 1.0) powerRatio = 1.0;

		int durationMs = 60; // Short window for smooth, continuous updates
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
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
			double angle = 2.0 * Math.PI * currentFreq * i / AudioUtils.SAMPLE_RATE;

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
		int numSamples = (AudioUtils.SAMPLE_RATE * totalDurationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random random = new Random();

		// Timing offsets in samples
		int tier1Duration = (AudioUtils.SAMPLE_RATE * 700) / 1000;  // First blast lasts 700ms
		int tier2Start = (AudioUtils.SAMPLE_RATE * 300) / 1000;     // Second blast starts at 300ms
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
			mixedSample = AudioUtils.clampToByte(mixedSample);

			buffer[i] = (byte) mixedSample;
		}

		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a massive, low-frequency white noise explosion mimicking 
	 * a sharp lightning strike crack followed by a deep rolling thunder decay.
	 */
	public static void playThunderSound() {
		int durationMs = 1200; // 1.2 seconds of rolling thunder
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random random = new Random();

		double lowPassFilter = 0.0;

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			double msElapsed = (double) i / AudioUtils.SAMPLE_RATE * 1000.0;

			// Generate raw white noise (-128 to 127)
			double noise = random.nextInt(256) - 128;

			// LAYER 1: The Initial Sharp Crack (First 80 milliseconds)
			// Higher frequency pass-through for an immediate lightning snap
			double crackSignal = 0.0;
			if (msElapsed < 80) {
				crackSignal = noise * 0.8 * (1.0 - (msElapsed / 80.0));
			}

			// LAYER 2: The Rolling Deep Rumble (Whole Duration)
			// Smooth low-pass filter to block highs and create bass structure
			lowPassFilter = lowPassFilter * 0.93 + noise * 0.07;
			
			// Non-linear decay envelope so the rumble trails off naturally
			double rumbleEnvelope = Math.pow(1.0 - progress, 3);
			double rumbleSignal = lowPassFilter * 1.2 * rumbleEnvelope;

			// Combine layers and maximize saturation bounds
			double finalSignal = (crackSignal + rumbleSignal) * 1.5;

			finalSignal = AudioUtils.clampToByte(finalSignal);

			buffer[i] = (byte) finalSignal;
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a massive, celestial impact sound effect.
	 * Features a high-velocity descending phase sweep (the meteor entry)
	 * followed by a saturated low-frequency kinetic white noise explosion.
	 */
	public static void playMeteorStrikeSound() {
		int durationMs = 1100; // 1.1 seconds of destructive entry and impact
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random random = new Random();

		// The entry streak phase lasts for the first 250 milliseconds
		int entrySamples = (AudioUtils.SAMPLE_RATE * 250) / 1000; 
		double lowPassFilter = 0.0;

		for (int i = 0; i < numSamples; i++) {
			double msElapsed = (double) i / AudioUtils.SAMPLE_RATE * 1000.0;

			double signal = 0.0;
			double rawNoise = random.nextInt(256) - 128; // Raw white noise source

			// --- PHASE 1: THE INCOMING PLASMA ENTRY STREAK ---
			if (i < entrySamples) {
				double entryProgress = (double) i / entrySamples;
				
				// Rapidly descending whistling sweep (starts at 1800Hz, drops to 150Hz)
				double entryFreq = 1800.0 * Math.pow(1.0 - entryProgress, 2) + 150.0;
				double entryAngle = 2.0 * Math.PI * entryFreq * i / AudioUtils.SAMPLE_RATE;
				
				// Triangle wave for a clean but piercing friction whistle tone
				double whistleWave = (Math.abs((entryAngle % (2.0 * Math.PI)) - Math.PI) / Math.PI) * 2.0 - 1.0;
				
				// Mix tone with high-pass filtered friction hiss
				signal += (whistleWave * 0.4 + (rawNoise * 0.3)) * entryProgress;
			}

			// --- PHASE 2: THE KINETIC IMPACT & SURFACE CRUSH ---
			// Triggers right at the 250ms threshold
			if (msElapsed >= 250.0) {
				double impactProgress = (msElapsed - 250.0) / (durationMs - 250.0);
				
				// Heavy low-pass filter mapping for ground displacement rumble
				lowPassFilter = lowPassFilter * 0.88 + rawNoise * 0.12;
				
				// Exponential volume drop-off as the shockwave disperses
				double impactEnvelope = Math.pow(1.0 - impactProgress, 3);
				
				signal += lowPassFilter * 1.5 * impactEnvelope;
			}

			// --- MASTER DIGITAL SATURATION DRIVE ---
			// Amplify to maximize the 8-bit clipping texture
			double masterSignal = signal * 1.4;

			masterSignal = AudioUtils.clampToByte(masterSignal);

			buffer[i] = (byte) masterSignal;
		}

		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a crisp, clear 8-bit chime mimicking a game pause event.
	 * Drops rapidly between two clean, high-register frequencies with a tight
	 * volume envelope to ensure an instant, responsive interface feel.
	 */
	public static void playPauseSound() {
		int durationMs = 120; // Short and distinct
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Two distinct steps: starts bright at 900Hz, then drops to 700Hz halfway through
			double frequency = (progress < 0.5) ? 900.0 : 700.0;
			double angle = 2.0 * Math.PI * frequency * i / AudioUtils.SAMPLE_RATE;

			// Square wave for classic chiptune crunch
			double squareWave = squareWave(angle);

			// Linear fade out for each half of the note structure to keep it punchy
			double noteProgress = (progress < 0.5) ? (progress / 0.5) : ((progress - 0.5) / 0.5);
			double volumeEnvelope = 1.0 - noteProgress;
			
			// Balanced volume scale (around 45) so it doesn't pierce the ears
			buffer[i] = (byte) (squareWave * 45.0 * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a crisp 8-bit chime for when the game is unpaused.
	 * Reverses the note order of the pause sound by stepping upward from 
	 * 700Hz to 900Hz, while keeping a clean, forward-fading volume decay.
	 */
	public static void playUnpauseSound() {
		int durationMs = 120; // Matches the pause duration exactly
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Reversed note order: starts low at 700Hz, then steps up to 900Hz halfway through
			double frequency = (progress < 0.5) ? 700.0 : 900.0;
			double angle = 2.0 * Math.PI * frequency * i / AudioUtils.SAMPLE_RATE;

			// Square wave for matching retro crunch
			double squareWave = squareWave(angle);

			// Forward decay: Each note strikes cleanly and fades out down to 0.0
			double noteProgress = (progress < 0.5) ? (progress / 0.5) : ((progress - 0.5) / 0.5);
			double volumeEnvelope = 1.0 - noteProgress; 
			
			// Balanced volume scale (around 45) to match the pause audio levels
			buffer[i] = (byte) (squareWave * 45.0 * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	/**
	 * Synthesizes a harsh, two-tone descending 8-bit buzz mimicking an error or invalid move.
	 * Drops from a dissonant mid-register tone down to a low buzz with a sharp decay.
	 */
	public static void playErrorSound() {
		int durationMs = 160; // Short and snappy error feedback
		int numSamples = (AudioUtils.SAMPLE_RATE * durationMs) / 1000;
		byte[] buffer = new byte[numSamples];
		Random rand = new Random();

		for (int i = 0; i < numSamples; i++) {
			double progress = (double) i / numSamples;
			
			// Two distinct descending tones: starts at a dissonant 300Hz, then drops to 150Hz
			double frequency = (progress < 0.4) ? 300.0 : 150.0;
			double angle = 2.0 * Math.PI * frequency * i / AudioUtils.SAMPLE_RATE;

			// Channel 1: Square wave for retro chiptune buzz
			double squareWave = squareWave(angle);
			
			// Channel 2: Sawtooth wave for additional abrasive texture
			double sawWave = (Math.abs((angle % (2.0 * Math.PI)) - Math.PI) / Math.PI) * 2.0 - 1.0;

			// Channel 3: Subtle noise bite on initial strike for punchiness
			double noiseBite = (rand.nextDouble() * 2.0 - 1.0) * (1.0 - progress);

			double mixedSignal = (squareWave * 0.5) + (sawWave * 0.3) + (noiseBite * 0.2);

			// Sharp exponential decay envelope so it cuts off cleanly
			double volumeEnvelope = Math.pow(1.0 - progress, 2);
			
			// Scaled amplitude (around 50) to make it clear without clipping or overwhelming other UI audio
			buffer[i] = (byte) (mixedSignal * 50.0 * volumeEnvelope);
		}
		playGeneratedSound(buffer);
	}
	
	// Helper Methods
	
	public static double squareWave(double angle) {
        return (Math.sin(angle) >= 0.0) ? 1.0 : -1.0;
    }
}