package com.utils;

import com.scorched.SoundEngine;

public class SoundPreview {

    public static void main(String[] args) {
        System.out.println("Initializing audio preview...");

        // 1. Trigger a sound effect from the SoundEngine
        System.out.println("Playing Menu Confirm Sound...");
        SoundEngine.playTwoTieredBlastSound();

        // 2. Prevent the application from closing immediately.
        // Because playMenuConfirmSound() creates an asynchronous daemon/worker thread,
        // we must pause the main thread to allow the audio buffer to finish playing.
        try {
            Thread.sleep(1000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Preview finished.");
    }
}