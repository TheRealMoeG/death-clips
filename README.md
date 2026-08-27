# Death Clips

A RuneLite Plugin Hub plugin that plays user-supplied death sounds whenever your local player dies.

No audio clips are bundled with the plugin. Every death sound is imported by the user and stored locally on that computer.

## Features

- **Selected** — always play one chosen sound.
- **Random** — choose a random sound on every death.
- **Shuffle bag** — play every installed sound once in a randomized order before reshuffling.
- **Prevent consecutive repeats** — avoids the same sound twice in a row when possible.
- **Volume control** — 0–100%.
- **Test Playback** — hear the current selection/mode without dying in-game.
- **Import WAV...** — add your own clips from the RuneLite sidebar.
- **Refresh** — rescan the local sound library.
- **Open Sounds Folder** — browse the local Death Clips audio folder from a user-initiated file chooser.
- **No bundled third-party audio** — the plugin ships with zero death sounds.

## How to use

The same instructions are shown at the bottom of the plugin sidebar.

1. Open **Death Clips** from the RuneLite sidebar.
2. Click **Import WAV...**.
3. Choose a WAV file from your computer.
4. Repeat to add as many sounds as you want, or use **Open Sounds Folder** to manage the folder directly.
5. Choose **Selected**, **Random**, or **Shuffle bag**.
6. Set the volume and click **Test Playback**.
7. When your local player dies, the plugin plays a sound using the selected mode.

## Audio format

Custom imports currently use WAV only so the plugin can stay dependency-free.

Recommended format:

- WAV
- PCM 16-bit
- 44.1 kHz or 48 kHz
- Mono or stereo

If a clip does not play, convert it to a standard PCM WAV and import it again.

## Local storage

Imported sounds are copied to:

```text
%USERPROFILE%\.runelite\death-clips\
```

The plugin stores imported audio inside its `.runelite` subdirectory. **Open Sounds Folder** opens a file browser rooted at that directory without launching external programs.


