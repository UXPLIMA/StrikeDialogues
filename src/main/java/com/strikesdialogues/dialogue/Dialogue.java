package com.strikesdialogues.dialogue;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Dialogue {

    private final String id;
    private final List<DialoguePage> pages;
    private final DialogueSettings settings;
    private final List<DialogueAction> endActions;

    public Dialogue(String id, List<DialoguePage> pages, DialogueSettings settings, List<DialogueAction> endActions) {
        this.id = id;
        this.pages = (pages != null) ? List.copyOf(pages) : Collections.emptyList();
        this.settings = (settings != null) ? settings : DialogueSettings.EMPTY;
        this.endActions = (endActions != null) ? List.copyOf(endActions) : Collections.emptyList();
    }

    public String getId() { return id; }
    public List<DialoguePage> getPages() { return pages; }
    public DialogueSettings getSettings() { return settings; }
    public List<DialogueAction> getEndActions() { return endActions; }

    public boolean isValid() {
        return id != null && !id.isBlank() && !pages.isEmpty();
    }

    public static class DialogueSettings {

        private final Optional<String> startSound;
        private final Optional<Float> startVolume;
        private final Optional<Float> startPitch;

        private final Optional<String> persistentBackgroundLine;
        private final Optional<Long> animationDelayTicks;
        private final Optional<BarColor> barColor;
        private final Optional<BarStyle> barStyle;
        private final Optional<String> typingSound;
        private final Optional<Float> typingVolume;
        private final Optional<Float> typingPitch;

        public static final DialogueSettings EMPTY = new DialogueSettings(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()
        );

        private DialogueSettings(Optional<String> persistentBackgroundLine, Optional<Long> animationDelayTicks, Optional<BarColor> barColor, Optional<BarStyle> barStyle,
                                 Optional<String> typingSound, Optional<Float> typingVolume, Optional<Float> typingPitch,
                                 Optional<String> startSound, Optional<Float> startVolume, Optional<Float> startPitch
        ) {
            this.persistentBackgroundLine = persistentBackgroundLine;
            this.animationDelayTicks = animationDelayTicks;
            this.barColor = barColor;
            this.barStyle = barStyle;
            this.typingSound = typingSound;
            this.typingVolume = typingVolume;
            this.typingPitch = typingPitch;
            this.startSound = startSound;
            this.startVolume = startVolume;
            this.startPitch = startPitch;
        }

        public static DialogueSettings create(String backgroundLine, Long delay, BarColor color, BarStyle style,
                                              String typingSound, Float typingVolume, Float typingPitch,
                                              String startSound, Float startVolume, Float startPitch
        ) {

            Optional<String> backgroundOpt = Optional.ofNullable(backgroundLine).filter(s -> !s.isBlank());
            Optional<Long> delayOpt = Optional.ofNullable(delay);
            Optional<BarColor> colorOpt = Optional.ofNullable(color);
            Optional<BarStyle> styleOpt = Optional.ofNullable(style);
            Optional<String> typingSoundOpt = Optional.ofNullable(typingSound).filter(s -> !s.isBlank());
            Optional<Float> typingVolumeOpt = Optional.ofNullable(typingVolume);
            Optional<Float> typingPitchOpt = Optional.ofNullable(typingPitch);
            Optional<String> startSoundOpt = Optional.ofNullable(startSound).filter(s -> !s.isBlank());
            Optional<Float> startVolumeOpt = Optional.ofNullable(startVolume);
            Optional<Float> startPitchOpt = Optional.ofNullable(startPitch);

            return new DialogueSettings(
                    backgroundOpt, delayOpt, colorOpt, styleOpt,
                    typingSoundOpt, typingVolumeOpt, typingPitchOpt,
                    startSoundOpt, startVolumeOpt, startPitchOpt
            );
        }

        public Optional<String> getPersistentBackgroundLine() { return persistentBackgroundLine; }
        public Optional<Long> getAnimationDelayTicks() { return animationDelayTicks; }
        public Optional<BarColor> getBarColor() { return barColor; }
        public Optional<BarStyle> getBarStyle() { return barStyle; }
        public Optional<String> getTypingSound() { return typingSound; }
        public Optional<Float> getTypingVolume() { return typingVolume; }
        public Optional<Float> getTypingPitch() { return typingPitch; }

        public Optional<String> getStartSound() { return startSound; }
        public Optional<Float> getStartVolume() { return startVolume; }
        public Optional<Float> getStartPitch() { return startPitch; }

        @Override
        public String toString() {
            return "DialogueSettings{" +
                    "persistentBackgroundLine=" + persistentBackgroundLine +
                    ", animationDelayTicks=" + animationDelayTicks +
                    ", barColor=" + barColor +
                    ", barStyle=" + barStyle +
                    ", typingSound=" + typingSound +
                    ", typingVolume=" + typingVolume +
                    ", typingPitch=" + typingPitch +
                    ", startSound=" + startSound +
                    ", startVolume=" + startVolume +
                    ", startPitch=" + startPitch +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Dialogue{" +
                "id='" + id + '\'' +
                ", pages=" + pages.size() +
                ", settings=" + settings +
                ", endActions=" + endActions.stream().map(a -> a.getType().name()).collect(Collectors.joining(", ")) +
                '}';
    }
}