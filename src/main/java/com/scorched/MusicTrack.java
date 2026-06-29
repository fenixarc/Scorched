package com.scorched;

public class MusicTrack {
	private String name;
	private double[] bassPattern;
	private double[] melodyPattern;
	private int[] drumsPattern;
	private int noteDurationMs;
	private int loopsBeforeMelody;
	private int loopsBeforeDrums;

	public MusicTrack(String name, double[] bassPattern, double[] melodyPattern, int[] drumsPattern,
			int noteDurationMs, int loopsBeforeMelody, int loopsBeforeDrums) {
		this.name = name;
		this.bassPattern = bassPattern;
		this.melodyPattern = melodyPattern;
		this.drumsPattern = drumsPattern;
		this.noteDurationMs = noteDurationMs;
		this.loopsBeforeMelody = loopsBeforeMelody;
		this.loopsBeforeDrums = loopsBeforeDrums;
	}

	public String getName() {
		return name;
	}

	public double[] getBassPattern() {
		return bassPattern;
	}

	public double[] getMelodyPattern() {
		return melodyPattern;
	}

	public int[] getDrumsPattern() {
		return drumsPattern;
	}

	public int getNoteDurationMs() {
		return noteDurationMs;
	}

	public int getLoopsBeforeMelody() {
		return loopsBeforeMelody;
	}

	public int getLoopsBeforeDrums() {
		return loopsBeforeDrums;
	}
}
