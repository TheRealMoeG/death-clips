package com.deathclips;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

@Singleton
final class DeathSoundLibrary
{
    private final File customSoundDirectory = new File(RuneLite.RUNELITE_DIR, "death-clips");
    private final List<DeathSound> sounds = new ArrayList<>();

    synchronized void refresh()
    {
        ensureCustomDirectory();
        sounds.clear();

        File[] files = customSoundDirectory.listFiles(file -> file.isFile() && isSupported(file.getName()));
        if (files == null)
        {
            return;
        }

        List<File> sorted = new ArrayList<>();
        Collections.addAll(sorted, files);
        sorted.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : sorted)
        {
            sounds.add(DeathSound.custom(file));
        }
    }

    synchronized List<DeathSound> getSounds()
    {
        return new ArrayList<>(sounds);
    }

    synchronized DeathSound getById(String id)
    {
        if (id != null && !id.isEmpty())
        {
            for (DeathSound sound : sounds)
            {
                if (sound.getId().equals(id))
                {
                    return sound;
                }
            }
        }
        return sounds.isEmpty() ? null : sounds.get(0);
    }

    synchronized int size()
    {
        return sounds.size();
    }

    File getCustomSoundDirectory()
    {
        ensureCustomDirectory();
        return customSoundDirectory;
    }

    File importWav(File source) throws IOException
    {
        if (source == null || !source.isFile())
        {
            throw new IOException("The selected file does not exist.");
        }
        if (!isSupported(source.getName()))
        {
            throw new IOException("Only .wav files are supported right now.");
        }

        ensureCustomDirectory();
        String fileName = source.getName();
        File destination = new File(customSoundDirectory, fileName);
        int suffix = 2;
        while (destination.exists())
        {
            int dot = fileName.lastIndexOf('.');
            String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
            String ext = dot > 0 ? fileName.substring(dot) : ".wav";
            destination = new File(customSoundDirectory, stem + " (" + suffix++ + ")" + ext);
        }

        Files.copy(source.toPath(), destination.toPath());
        refresh();
        return destination;
    }

    private void ensureCustomDirectory()
    {
        if (!customSoundDirectory.exists())
        {
            customSoundDirectory.mkdirs();
        }
    }

    private static boolean isSupported(String name)
    {
        return name.toLowerCase(Locale.ROOT).endsWith(".wav");
    }
}
