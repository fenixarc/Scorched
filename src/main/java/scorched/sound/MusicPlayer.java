package scorched.sound;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class MusicPlayer {
	
	private static volatile boolean playMusic = false;
	private static Thread musicThread;
	
	public static final AtomicInteger musicVolume = new AtomicInteger(7);     // Scale from 1 to 10
	public static final AtomicBoolean muteMusic = new AtomicBoolean(false);
	
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

		playMusic = true;

		System.out.println("Now Playing: " + track.getName());

		musicThread = new Thread(() -> {
			// Extract patterns from track
			double[] bassPattern = track.getBassPattern();
			double[] melodyPattern = track.getMelodyPattern();
			int[] drumsPattern = track.getDrumsPattern();
			double[] synthPattern = track.getSynthPattern();

			int samplesPerStep = (AudioUtils.SAMPLE_RATE * track.getNoteDurationMs()) / 1000;
			Random rand = new Random();

			try {
				SourceDataLine line = AudioSystem.getSourceDataLine(AudioUtils.AUDIO_FORMAT);
				line.open(AudioUtils.AUDIO_FORMAT, AudioUtils.BUFFER_SIZE);
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
							double bassAngle = 2.0 * Math.PI * bassFreq * i / AudioUtils.SAMPLE_RATE;
							totalSignal += Math.sin(bassAngle) * track.getBassVolumeModifier() * volumeEnvelope;
						}

						// CHANNEL 2: Melody Lead (Square Wave)
						if (melodyActive && melodyFreq > 0.0) {
							double melodyAngle = 2.0 * Math.PI * melodyFreq * i / AudioUtils.SAMPLE_RATE;
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
							double synthAngle = 2.0 * Math.PI * synthFreq * i / AudioUtils.SAMPLE_RATE;
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
						
						// Apply global music settings override
						if (muteMusic.get()) {
							totalSignal = 0.0;
						} else {
							totalSignal *= (musicVolume.get() / 10.0);
						}


						// Protect audio driver from overflow clipping bounds
						totalSignal = AudioUtils.clampToByte(totalSignal);

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

}
