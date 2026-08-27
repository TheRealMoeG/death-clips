package com.deathclips;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
final class DeathClipsAudioPlayer
{
    private static final Logger log = LoggerFactory.getLogger(DeathClipsAudioPlayer.class);

    private final AudioPlayer audioPlayer;
    private ExecutorService executor;

    @Inject
    DeathClipsAudioPlayer(AudioPlayer audioPlayer)
    {
        this.audioPlayer = audioPlayer;
    }

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
        if (executor != null)
        {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void playBlocking(DeathSound sound, int volume)
    {
        try (InputStream stream = sound.openStream())
        {
            audioPlayer.play(stream, volumeToGain(volume));
        }
        catch (Exception ex)
        {
            log.warn("Unable to play Death Clips sound {}", sound.getDisplayName(), ex);
        }
    }

    private static float volumeToGain(int volume)
    {
        if (volume >= 100)
        {
            return 0.0f;
        }

        double linear = Math.max(volume / 100.0, 0.01);
        return (float) (20.0 * Math.log10(linear));
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
