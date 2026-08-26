package com.deathclips;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(DeathClipsConfig.GROUP)
public interface DeathClipsConfig extends Config
{
    String GROUP = "deathclips";

    @ConfigSection(
        name = "Playback",
        description = "Death sound playback behavior",
        position = 0
    )
    String playbackSection = "playback";

    @ConfigItem(
        keyName = "mode",
        name = "Sound mode",
        description = "Choose one sound, pure random, or shuffle bag",
        position = 0,
        section = playbackSection
    )
    default SoundMode mode()
    {
        return SoundMode.SHUFFLE;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "volume",
        name = "Volume",
        description = "Death sound volume from 0 to 100",
        position = 1,
        section = playbackSection
    )
    default int volume()
    {
        return 80;
    }

    @ConfigItem(
        keyName = "preventRepeats",
        name = "Prevent repeats",
        description = "Avoid playing the same clip twice in a row when possible",
        position = 2,
        section = playbackSection
    )
    default boolean preventRepeats()
    {
        return true;
    }

    @ConfigItem(
        keyName = "selectedSoundId",
        name = "Selected sound ID",
        description = "Internal selected sound identifier",
        hidden = true
    )
    default String selectedSoundId()
    {
        return "";
    }
}
