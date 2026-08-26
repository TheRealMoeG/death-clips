package com.deathclips;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

final class DeathSound
{
    private final String id;
    private final String displayName;
    private final File file;

    private DeathSound(String id, String displayName, File file)
    {
        this.id = id;
        this.displayName = displayName;
        this.file = file;
    }

    static DeathSound custom(File file)
    {
        String name = file.getName();
        String id = "custom:" + name.toLowerCase(Locale.ROOT);
        return new DeathSound(id, prettyName(name), file);
    }

    String getId()
    {
        return id;
    }

    String getDisplayName()
    {
        return displayName;
    }

    InputStream openStream() throws FileNotFoundException
    {
        return new BufferedInputStream(new FileInputStream(file));
    }

    private static String prettyName(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        String raw = dot > 0 ? fileName.substring(0, dot) : fileName;
        raw = raw.replace('_', ' ').replace('-', ' ').trim();
        if (raw.isEmpty())
        {
            return fileName;
        }

        String[] words = raw.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words)
        {
            if (out.length() > 0)
            {
                out.append(' ');
            }
            if (word.length() <= 3 && word.equals(word.toUpperCase()))
            {
                out.append(word);
            }
            else
            {
                out.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1)
                {
                    out.append(word.substring(1));
                }
            }
        }
        return out.toString();
    }

    @Override
    public String toString()
    {
        return displayName;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof DeathSound))
        {
            return false;
        }
        DeathSound other = (DeathSound) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
