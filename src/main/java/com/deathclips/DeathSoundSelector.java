package com.deathclips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class DeathSoundSelector
{
    private final DeathSoundLibrary library;
    private final Random random = new Random();
    private final List<String> shuffleBag = new ArrayList<>();
    private Set<String> shuffleSnapshot = Collections.emptySet();
    private String lastPlayedId;

    @Inject
    DeathSoundSelector(DeathSoundLibrary library)
    {
        this.library = library;
    }

    synchronized DeathSound choose(SoundMode mode, String selectedId, boolean preventRepeats)
    {
        List<DeathSound> sounds = library.getSounds();
        if (sounds.isEmpty())
        {
            return null;
        }

        DeathSound chosen;
        if (mode == SoundMode.SELECTED)
        {
            chosen = library.getById(selectedId);
        }
        else if (mode == SoundMode.RANDOM)
        {
            chosen = chooseRandom(sounds, preventRepeats);
        }
        else
        {
            chosen = chooseShuffle(sounds, preventRepeats);
        }

        if (chosen != null)
        {
            lastPlayedId = chosen.getId();
        }
        return chosen;
    }

    synchronized void reset()
    {
        shuffleBag.clear();
        shuffleSnapshot = Collections.emptySet();
        lastPlayedId = null;
    }

    private DeathSound chooseRandom(List<DeathSound> sounds, boolean preventRepeats)
    {
        if (!preventRepeats || sounds.size() <= 1 || lastPlayedId == null)
        {
            return sounds.get(random.nextInt(sounds.size()));
        }

        List<DeathSound> candidates = new ArrayList<>();
        for (DeathSound sound : sounds)
        {
            if (!sound.getId().equals(lastPlayedId))
            {
                candidates.add(sound);
            }
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private DeathSound chooseShuffle(List<DeathSound> sounds, boolean preventRepeats)
    {
        Set<String> currentIds = new HashSet<>();
        for (DeathSound sound : sounds)
        {
            currentIds.add(sound.getId());
        }

        if (shuffleBag.isEmpty() || !currentIds.equals(shuffleSnapshot))
        {
            shuffleBag.clear();
            shuffleBag.addAll(currentIds);
            Collections.shuffle(shuffleBag, random);
            shuffleSnapshot = currentIds;

            if (preventRepeats && shuffleBag.size() > 1 && lastPlayedId != null
                && shuffleBag.get(0).equals(lastPlayedId))
            {
                Collections.swap(shuffleBag, 0, 1);
            }
        }

        String id = shuffleBag.remove(0);
        return library.getById(id);
    }
}
