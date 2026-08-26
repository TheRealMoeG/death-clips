package com.deathclips;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Singleton
public final class DeathClipsPanel extends PluginPanel
{
    private static final int CONTROL_HEIGHT = 28;

    private final DeathSoundLibrary library;
    private final DeathSoundSelector selector;
    private final DeathClipsAudioPlayer audioPlayer;
    private final DeathClipsConfig config;
    private final ConfigManager configManager;

    private final JComboBox<SoundMode> modeCombo = new JComboBox<>(SoundMode.values());
    private final JComboBox<DeathSound> soundCombo = new JComboBox<>();
    private final JSlider volumeSlider = new JSlider(0, 100, 80);
    private final JLabel volumeValue = new JLabel("80%", SwingConstants.RIGHT);
    private final JCheckBox preventRepeats = new JCheckBox("Prevent consecutive repeats");
    private final JLabel installedCount = new JLabel();
    private final JLabel statusLabel = new JLabel("Import a WAV to get started");
    private JButton testButton;
    private boolean updating;

    @Inject
    public DeathClipsPanel(
        DeathSoundLibrary library,
        DeathSoundSelector selector,
        DeathClipsAudioPlayer audioPlayer,
        DeathClipsConfig config,
        ConfigManager configManager)
    {
        // Use PluginPanel's native wrapper. RuneLite supplies the 225px width and scrolling.
        super();
        this.library = library;
        this.selector = selector;
        this.audioPlayer = audioPlayer;
        this.config = config;
        this.configManager = configManager;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        configureControls();

        add(buildHeader());
        add(Box.createVerticalStrut(10));
        add(buildPlaybackCard());
        add(Box.createVerticalStrut(10));
        add(buildLibraryCard());
        add(Box.createVerticalStrut(10));
        add(buildStatusCard());
        add(Box.createVerticalStrut(10));
        add(buildInstructionsCard());
        add(Box.createVerticalGlue());

        installListeners();
        refreshFromLibrary();
        syncFromConfig();
    }

    private void configureControls()
    {
        configureCombo(modeCombo);
        configureCombo(soundCombo);
        soundCombo.setRenderer(new SoundRenderer());

        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);

        preventRepeats.setOpaque(false);
        preventRepeats.setForeground(Color.WHITE);
        preventRepeats.setFont(FontManager.getRunescapeSmallFont());
        preventRepeats.setFocusPainted(false);
        preventRepeats.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void configureCombo(JComboBox<?> combo)
    {
        combo.setFocusable(false);
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setMinimumSize(new Dimension(0, CONTROL_HEIGHT));
        combo.setPreferredSize(new Dimension(170, CONTROL_HEIGHT));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, CONTROL_HEIGHT));
    }

    void refreshFromLibrary()
    {
        updating = true;
        String selectedId = config.selectedSoundId();
        List<DeathSound> sounds = library.getSounds();
        DefaultComboBoxModel<DeathSound> model = new DefaultComboBoxModel<>();
        for (DeathSound sound : sounds)
        {
            model.addElement(sound);
        }
        soundCombo.setModel(model);

        DeathSound selected = library.getById(selectedId);
        if (selected != null)
        {
            soundCombo.setSelectedItem(selected);
        }

        installedCount.setText(Integer.toString(library.size()));
        updating = false;
    }

    void syncFromConfig()
    {
        updating = true;
        modeCombo.setSelectedItem(config.mode());
        volumeSlider.setValue(config.volume());
        volumeValue.setText(config.volume() + "%");
        preventRepeats.setSelected(config.preventRepeats());

        DeathSound selected = library.getById(config.selectedSoundId());
        if (selected != null)
        {
            soundCombo.setSelectedItem(selected);
        }
        updating = false;
        updateModeUi();
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JLabel title = new JLabel("Death Clips");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brand = new JLabel("CUSTOM AUDIO");
        brand.setForeground(ColorScheme.BRAND_ORANGE);
        brand.setFont(FontManager.getRunescapeBoldFont());
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Make every death memorable.");
        subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        subtitle.setFont(FontManager.getRunescapeSmallFont());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(brand);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);
        return header;
    }

    private JPanel buildPlaybackCard()
    {
        JPanel card = makeCard("PLAYBACK");
        JPanel body = cardBody(card);

        body.add(makeStackedField("MODE", modeCombo));
        body.add(Box.createVerticalStrut(9));
        body.add(makeStackedField("SOUND", soundCombo));
        body.add(Box.createVerticalStrut(10));

        JPanel volumeRow = new JPanel(new BorderLayout(8, 0));
        volumeRow.setOpaque(false);
        volumeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel volumeLabel = makeMutedLabel("VOLUME");
        volumeValue.setForeground(Color.WHITE);
        volumeValue.setFont(FontManager.getRunescapeSmallFont());
        volumeRow.add(volumeLabel, BorderLayout.WEST);
        volumeRow.add(volumeValue, BorderLayout.EAST);
        body.add(volumeRow);
        body.add(Box.createVerticalStrut(2));

        volumeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        volumeSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        body.add(volumeSlider);
        body.add(Box.createVerticalStrut(4));

        body.add(preventRepeats);
        body.add(Box.createVerticalStrut(10));

        testButton = makePrimaryButton("▶  TEST PLAYBACK");
        testButton.addActionListener(e -> playTest());
        body.add(testButton);

        return card;
    }

    private JPanel buildLibraryCard()
    {
        JPanel card = makeCard("SOUND LIBRARY");
        JPanel body = cardBody(card);

        JPanel stats = new JPanel(new GridLayout(1, 2, 6, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        stats.add(makeStat("INSTALLED", installedCount));
        JLabel formatValue = new JLabel("WAV");
        stats.add(makeStat("FORMAT", formatValue));
        body.add(stats);
        body.add(Box.createVerticalStrut(9));

        JButton importButton = new JButton("Import WAV...");
        importButton.setFocusable(false);
        importButton.addActionListener(e -> importWav());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(e ->
        {
            library.refresh();
            selector.reset();
            refreshFromLibrary();
            syncFromConfig();
            if (library.size() > 0)
            {
                setStatus("Library refreshed");
            }
        });

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 6, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        actionRow.add(importButton);
        actionRow.add(refreshButton);
        body.add(actionRow);
        body.add(Box.createVerticalStrut(6));

        JButton openFolderButton = new JButton("Open Sounds Folder");
        openFolderButton.setFocusable(false);
        openFolderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        openFolderButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        openFolderButton.addActionListener(e -> openSoundsFolder());
        body.add(openFolderButton);
        body.add(Box.createVerticalStrut(6));

        JLabel note = new JLabel("<html>Your sounds stay local on this PC.<br>Import WAVs or open the folder to manage them.</html>");
        note.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        note.setFont(FontManager.getRunescapeSmallFont());
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(note);

        return card;
    }

    private JPanel buildInstructionsCard()
    {
        JPanel card = makeCard("HOW TO USE");
        JPanel body = cardBody(card);

        JLabel steps = new JLabel(
            "<html>"
                + "<b>1.</b> Click <b>Import WAV...</b><br>"
                + "<b>2.</b> Add one or more death sounds<br>"
                + "<b>3.</b> Pick Selected, Random, or Shuffle<br>"
                + "<b>4.</b> Use Test Playback to check volume<br>"
                + "<b>5.</b> Die in-game. RuneLite handles the rest."
                + "</html>");
        steps.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        steps.setFont(FontManager.getRunescapeSmallFont());
        steps.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(steps);
        body.add(Box.createVerticalStrut(8));

        JLabel format = new JLabel("<html><b>Audio:</b> WAV • PCM 16-bit recommended</html>");
        format.setForeground(ColorScheme.BRAND_ORANGE);
        format.setFont(FontManager.getRunescapeSmallFont());
        format.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(format);

        return card;
    }

    private JPanel buildStatusCard()
    {
        JPanel card = new JPanel(new BorderLayout(7, 0));
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(new EmptyBorder(8, 9, 8, 9));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel dot = new JLabel("●");
        dot.setForeground(ColorScheme.BRAND_ORANGE);
        dot.setFont(FontManager.getRunescapeSmallFont());

        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setFont(FontManager.getRunescapeSmallFont());

        card.add(dot, BorderLayout.WEST);
        card.add(statusLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel makeCard(String titleText)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(8, 9, 9, 9)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel title = new JLabel(titleText);
        title.setForeground(ColorScheme.BRAND_ORANGE);
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.add(body);
        return card;
    }

    private JPanel cardBody(JPanel card)
    {
        return (JPanel) card.getComponent(card.getComponentCount() - 1);
    }

    private JPanel makeStackedField(String labelText, JComponent control)
    {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 47));

        JLabel label = makeMutedLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        control.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(3));
        panel.add(control);
        return panel;
    }

    private JLabel makeMutedLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(FontManager.getRunescapeSmallFont());
        return label;
    }

    private JPanel makeStat(String name, JLabel value)
    {
        JPanel panel = new JPanel();
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        value.setForeground(Color.WHITE);
        value.setFont(FontManager.getRunescapeBoldFont().deriveFont(17f));
        value.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel(name);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(value);
        panel.add(label);
        return panel;
    }

    private JButton makePrimaryButton(String text)
    {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeBoldFont());
        button.setFocusable(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private void installListeners()
    {
        modeCombo.addActionListener(e ->
        {
            if (updating)
            {
                return;
            }
            SoundMode mode = (SoundMode) modeCombo.getSelectedItem();
            if (mode != null)
            {
                configManager.setConfiguration(DeathClipsConfig.GROUP, "mode", mode);
                selector.reset();
                updateModeUi();
            }
        });

        soundCombo.addActionListener(e ->
        {
            if (updating)
            {
                return;
            }
            DeathSound sound = (DeathSound) soundCombo.getSelectedItem();
            if (sound != null)
            {
                configManager.setConfiguration(DeathClipsConfig.GROUP, "selectedSoundId", sound.getId());
                setStatus("Selected: " + sound.getDisplayName());
            }
        });

        volumeSlider.addChangeListener(e ->
        {
            int volume = volumeSlider.getValue();
            volumeValue.setText(volume + "%");
            if (!updating && !volumeSlider.getValueIsAdjusting())
            {
                configManager.setConfiguration(DeathClipsConfig.GROUP, "volume", volume);
            }
        });

        preventRepeats.addActionListener(e ->
        {
            if (!updating)
            {
                configManager.setConfiguration(
                    DeathClipsConfig.GROUP,
                    "preventRepeats",
                    preventRepeats.isSelected());
                selector.reset();
            }
        });
    }

    private void playTest()
    {
        SoundMode mode = (SoundMode) modeCombo.getSelectedItem();
        DeathSound sound;

        if (mode == null || mode == SoundMode.SELECTED)
        {
            sound = (DeathSound) soundCombo.getSelectedItem();
        }
        else
        {
            DeathSound selected = (DeathSound) soundCombo.getSelectedItem();
            String selectedId = selected == null ? config.selectedSoundId() : selected.getId();
            sound = selector.choose(mode, selectedId, preventRepeats.isSelected());
        }

        if (sound == null)
        {
            setStatus("No sound available");
            return;
        }

        audioPlayer.play(sound, volumeSlider.getValue());
        setStatus("Playing: " + sound.getDisplayName());
    }

    private void importWav()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import a custom death sound");
        chooser.setFileFilter(new FileNameExtensionFilter("WAV audio (*.wav)", "wav"));
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        try
        {
            File imported = library.importWav(chooser.getSelectedFile());
            selector.reset();
            refreshFromLibrary();
            DeathSound sound = library.getById("custom:" + imported.getName().toLowerCase(Locale.ROOT));
            if (sound != null)
            {
                soundCombo.setSelectedItem(sound);
                configManager.setConfiguration(DeathClipsConfig.GROUP, "selectedSoundId", sound.getId());
            }
            setStatus("Imported: " + imported.getName());
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Import failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Import failed");
        }
    }

    private void openSoundsFolder()
    {
        File folder = library.getCustomSoundDirectory();
        JFileChooser chooser = new JFileChooser(folder);
        chooser.setDialogTitle("Death Clips sounds folder");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(true);

        // RuneLite Plugin Hub guidelines permit JFileChooser for user-initiated
        // file operations. Opening the browser at our .runelite subdirectory
        // gives users direct access to their sound library without Desktop APIs.
        chooser.showOpenDialog(this);
        setStatus("Sounds folder browser opened");
    }

    private void updateModeUi()
    {
        SoundMode mode = (SoundMode) modeCombo.getSelectedItem();
        boolean hasSounds = library.size() > 0;
        boolean selectedMode = mode == SoundMode.SELECTED;
        soundCombo.setEnabled(hasSounds && selectedMode);
        if (testButton != null)
        {
            testButton.setEnabled(hasSounds);
        }

        if (!hasSounds)
        {
            setStatus("Import a WAV to get started");
        }
        else if (selectedMode)
        {
            setStatus("Selected mode: chosen sound");
        }
        else if (mode == SoundMode.RANDOM)
        {
            setStatus("Random mode: new roll each death");
        }
        else
        {
            setStatus("Shuffle: all sounds before repeats");
        }
    }

    private void setStatus(String text)
    {
        statusLabel.setText(text);
        statusLabel.setToolTipText(text);
    }

    private static final class SoundRenderer extends JLabel implements ListCellRenderer<DeathSound>
    {
        SoundRenderer()
        {
            setOpaque(true);
            setBorder(new EmptyBorder(3, 5, 3, 5));
        }

        @Override
        public Component getListCellRendererComponent(
            JList<? extends DeathSound> list,
            DeathSound value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            String name = value == null ? "" : value.getDisplayName();
            setText(name);
            setToolTipText(name);

            if (isSelected)
            {
                setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                setForeground(Color.WHITE);
            }
            else
            {
                setBackground(ColorScheme.DARKER_GRAY_COLOR);
                setForeground(Color.WHITE);
            }
            setFont(FontManager.getRunescapeSmallFont());
            return this;
        }
    }
}
