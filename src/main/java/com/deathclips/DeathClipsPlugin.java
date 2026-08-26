package com.deathclips;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "Death Clips",
    description = "Play your own selectable, random, or shuffled death sounds",
    tags = {"death", "sound", "audio", "custom", "random", "shuffle"}
)
public class DeathClipsPlugin extends Plugin
{
    private static final long DUPLICATE_EVENT_COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(3);

    @Inject
    private Client client;

    @Inject
    private DeathClipsConfig config;

    @Inject
    private DeathSoundLibrary library;

    @Inject
    private DeathSoundSelector selector;

    @Inject
    private DeathClipsAudioPlayer audioPlayer;

    @Inject
    private ClientToolbar clientToolbar;

    private DeathClipsPanel panel;
    private NavigationButton navButton;
    private long lastDeathSoundNanos;

    @Override
    protected void startUp()
    {
        library.refresh();
        selector.reset();
        audioPlayer.startUp();
        lastDeathSoundNanos = 0L;

        panel = injector.getInstance(DeathClipsPanel.class);
        panel.refreshFromLibrary();
        panel.syncFromConfig();

        BufferedImage icon = ImageUtil.loadImageResource(DeathClipsPlugin.class, "panel_icon.png");
        navButton = NavigationButton.builder()
            .tooltip("Death Clips")
            .priority(8)
            .icon(icon)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        panel = null;
        audioPlayer.shutDown();
        selector.reset();
        lastDeathSoundNanos = 0L;
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (event.getActor() != client.getLocalPlayer())
        {
            return;
        }

        long now = System.nanoTime();
        if (now - lastDeathSoundNanos < DUPLICATE_EVENT_COOLDOWN_NANOS)
        {
            return;
        }
        lastDeathSoundNanos = now;

        DeathSound sound = selector.choose(config.mode(), config.selectedSoundId(), config.preventRepeats());
        audioPlayer.play(sound, config.volume());
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!DeathClipsConfig.GROUP.equals(event.getGroup()) || panel == null)
        {
            return;
        }

        SwingUtilities.invokeLater(panel::syncFromConfig);
    }

    @Provides
    DeathClipsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DeathClipsConfig.class);
    }
}
