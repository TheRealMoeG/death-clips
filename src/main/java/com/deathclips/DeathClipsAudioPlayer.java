package com.deathclips;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

@Singleton
final class DeathClipsAudioPlayer
{
    private final Object clipLock = new Object();
    private ExecutorService executor;
    private Clip activeClip;

    synchronized void startUp()
    {
        if (executor == null || executor.isShutdown())
        {
            executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
        }
    }

    void play(DeathSound sound, int volume)
    {
        if (sound == null || volume <= 0)
        {
            return;
        }

        ExecutorService current;
        synchronized (this)
        {
            if (executor == null || executor.isShutdown())
            {
                return;
            }
            current = executor;
        }

        try
        {
            current.execute(() -> playBlocking(sound, Math.min(volume, 100)));
        }
        catch (RejectedExecutionException ignored)
        {
            // Plugin is shutting down.
        }
    }

    synchronized void shutDown()
    {
        stopActiveClip();
        if (executor != null)
        {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void playBlocking(DeathSound sound, int volume)
    {
        stopActiveClip();

        try (InputStream raw = sound.openStream())
        {
            if (raw == null)
            {
                System.err.println("Death Clips: missing audio for " + sound.getDisplayName());
                return;
            }

            try (AudioInputStream audio = AudioSystem.getAudioInputStream(raw))
            {
                Clip clip = AudioSystem.getClip();
                clip.open(audio);
                applyVolume(clip, volume);

                synchronized (clipLock)
                {
                    activeClip = clip;
                }

                clip.addLineListener(event ->
                {
                    if (event.getType() == LineEvent.Type.STOP)
                    {
                        clip.close();
                        synchronized (clipLock)
                        {
                            if (activeClip == clip)
                            {
                                activeClip = null;
                            }
                        }
                    }
                });

                clip.start();
            }
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex)
        {
            System.err.println("Death Clips: unable to play " + sound.getDisplayName() + ": " + ex.getMessage());
        }
    }

    private static void applyVolume(Clip clip, int volume)
    {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
        {
            return;
        }

        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float linear = Math.max(volume / 100.0f, 0.0001f);
        float decibels = (float) (20.0 * Math.log10(linear));
        decibels = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels));
        gain.setValue(decibels);
    }

    private void stopActiveClip()
    {
        synchronized (clipLock)
        {
            if (activeClip != null)
            {
                activeClip.stop();
                activeClip.close();
                activeClip = null;
            }
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread(Runnable runnable)
        {
            Thread thread = new Thread(runnable, "death-clips-audio");
            thread.setDaemon(true);
            return thread;
        }
    }
}
