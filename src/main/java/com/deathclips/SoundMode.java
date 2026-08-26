package com.deathclips;

public enum SoundMode
{
    SELECTED("Selected sound"),
    RANDOM("Random"),
    SHUFFLE("Shuffle bag");

    private final String displayName;

    SoundMode(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
