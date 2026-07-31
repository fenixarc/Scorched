package scorched.sound;

import javax.sound.sampled.AudioFormat;

public class AudioUtils {
	
	public static final int SAMPLE_RATE = 16000; // 16kHz audio quality
	public static final int BUFFER_SIZE = 1024;
	public static final AudioFormat AUDIO_FORMAT = new AudioFormat(AudioUtils.SAMPLE_RATE, 8, 1, true, false);
	
	public static byte clampToByte(double val) {
        if (val > 127.0) return 127;
        if (val < -128.0) return -128;
        return (byte) val;
    }

}
