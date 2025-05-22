package com.strikesdialogues.config;

import com.strikesdialogues.StrikesDialogues;
import com.strikesdialogues.dialogue.Dialogue;
import com.strikesdialogues.dialogue.DialogueAction;
import com.strikesdialogues.dialogue.DialoguePage;
import com.strikesdialogues.util.CharacterMapper;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigManager {

    private final StrikesDialogues plugin;
    private FileConfiguration config;

    private String defaultStartSound;
    private float defaultStartVolume;
    private float defaultStartPitch;
    private String globalPersistentBackgroundLine;
    private long defaultAnimationDelayTicks;
    private BarColor defaultBarColor;
    private BarStyle defaultBarStyle;
    private String defaultTypingSound;
    private float defaultTypingVolume;
    private float defaultTypingPitch;
    private long defaultHoldTimeTicks;
    private boolean movementRestrictionEnabled;
    private boolean lockPerspectiveEnabled;
    private boolean papiEnabled;
    private boolean debugLogging;
    private int customCommandMaxLines;
    private String customCommandBackgroundChar;
    private long endActionDelayTicks;

    private String msgNoPermission;
    private String msgPlayerOnly;
    private String msgDialogueNotFound;
    private String msgCustomLineLimit;
    private String msgReloadSuccess;
    private String msgReloadFail;
    private String msgMustBeOnGround;

    private final Map<String, Dialogue> loadedDialogues = new HashMap<>();
    private final Set<String> invalidPathsFound = new HashSet<>();

    private static final List<String> FORBIDDEN_LEGACY_CODES = Arrays.asList("&k", "&m", "&n", "&o");
    private static final List<String> FORBIDDEN_MINI_TAGS = Arrays.asList(
            "<obf>", "<magic>", "<strikethrough>", "<st>", "<underline>", "<u>", "<italic>", "<i>"
    );

    public ConfigManager(StrikesDialogues plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        this.invalidPathsFound.clear();
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadGlobalSettings();
        loadDialogues();
        plugin.getLogger().info("Loaded " + loadedDialogues.size() + " dialogues.");
    }

    private String validateCodesAndSymbols(String value, String path) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        boolean issueFound = false;
        String lowerCaseValue = value.toLowerCase();

        for (String code : FORBIDDEN_LEGACY_CODES) {
            if (lowerCaseValue.contains(code)) {
                issueFound = true;
                break;
            }
        }

        if (!issueFound) {
            for (String tag : FORBIDDEN_MINI_TAGS) {
                if (lowerCaseValue.contains(tag)) {
                    issueFound = true;
                    break;
                }
            }
        }

        if (!issueFound) {
            int index = 0;
            int len = value.length();
            while (index < len) {
                char currentChar = value.charAt(index);
                int charsToSkip = 0;

                if ((currentChar == '&' || currentChar == '§') && index + 1 < len) {
                    char nextChar = value.charAt(index + 1);
                    if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(nextChar) != -1) charsToSkip = 2;
                    else if (nextChar == '#' && index + 7 < len) charsToSkip = 8;
                    else if ((nextChar == 'x' || nextChar == 'X') && index + 13 < len) charsToSkip = 14;
                    else charsToSkip = 1;
                } else if (currentChar == '<') {
                    int closingBracket = value.indexOf('>', index);
                    if (closingBracket > index) {
                        String potentialTag = value.substring(index, closingBracket + 1).toLowerCase();
                        if (FORBIDDEN_MINI_TAGS.contains(potentialTag)) {
                            issueFound = true;
                            break;
                        }
                        charsToSkip = (closingBracket - index) + 1;
                    }
                }

                if (issueFound) break;

                if (charsToSkip > 0) {
                    index += charsToSkip;
                    continue;
                }

                if (!CharacterMapper.isCharacterSupported(currentChar)) {
                    issueFound = true;
                    break;
                }

                index++;
            }
        }

        if (issueFound) {
            this.invalidPathsFound.add(path);
        }
        return value;
    }

    public Set<String> getInvalidPathsFound() {
        return this.invalidPathsFound;
    }


    private void loadGlobalSettings() {
        ConfigurationSection settings = config.getConfigurationSection("dialogue-settings");
        if (settings == null) {
            plugin.getLogger().warning("Missing 'dialogue-settings' section in config.yml! Using default values.");
            settings = config.createSection("dialogue-settings");
        }

        globalPersistentBackgroundLine = validateCodesAndSymbols(settings.getString("persistent-background-line", ""), "dialogue-settings.persistent-background-line");
        defaultAnimationDelayTicks = Math.max(1L, settings.getLong("default-animation-delay-ticks", 2L));
        defaultBarColor = safeGetEnum(settings, "default-bar-color", BarColor.class, BarColor.BLUE);
        defaultBarStyle = safeGetEnum(settings, "default-bar-style", BarStyle.class, BarStyle.SOLID);

        if (settings.isConfigurationSection("default-typing-sound")) {
            defaultTypingSound = settings.getString("default-typing-sound.sound", "");
            defaultTypingVolume = (float) settings.getDouble("default-typing-sound.volume", 0.3);
            defaultTypingPitch = (float) settings.getDouble("default-typing-sound.pitch", 1.8);
        } else {
            defaultTypingSound = settings.getString("default-typing-sound", "ui.button.click");
            defaultTypingVolume = 0.3f;
            defaultTypingPitch = 1.8f;
        }
        if(defaultTypingSound != null && defaultTypingSound.isBlank()) defaultTypingSound = null;

        if (settings.isConfigurationSection("default-start-sound")) {
            defaultStartSound = settings.getString("default-start-sound.sound", null);
            defaultStartVolume = (float) settings.getDouble("default-start-sound.volume", 1.0);
            defaultStartPitch = (float) settings.getDouble("default-start-sound.pitch", 1.0);
        } else {
            defaultStartSound = settings.getString("default-start-sound", null);
            defaultStartVolume = 1.0f;
            defaultStartPitch = 1.0f;
        }
        if (defaultStartSound != null && defaultStartSound.isBlank()) defaultStartSound = null;

        defaultHoldTimeTicks = Math.max(0L, settings.getLong("default-hold-time-ticks", 40L));
        movementRestrictionEnabled = settings.getBoolean("enable-movement-restriction", true);
        lockPerspectiveEnabled = settings.getBoolean("lock-perspective", true);
        papiEnabled = settings.getBoolean("placeholderapi-enabled", true);
        debugLogging = settings.getBoolean("debug-logging", false);
        customCommandMaxLines = Math.max(1, settings.getInt("custom-command-max-lines", 4));
        customCommandBackgroundChar = validateCodesAndSymbols(settings.getString("custom-command-background-char", "&f"), "dialogue-settings.custom-command-background-char");
        endActionDelayTicks = Math.max(0L, settings.getLong("end-action-delay-ticks", 20L));

        ConfigurationSection messages = config.getConfigurationSection("plugin-messages");
        if (messages == null) {
            plugin.getLogger().warning("Missing 'plugin-messages' section in config.yml! Using default messages.");
            messages = config.createSection("plugin-messages");
        }
        msgNoPermission = validateCodesAndSymbols(messages.getString("no-permission", "&cYou don't have permission."), "plugin-messages.no-permission");
        msgPlayerOnly = validateCodesAndSymbols(messages.getString("player-only", "&cCommand must be run by a player."), "plugin-messages.player-only");
        msgDialogueNotFound = validateCodesAndSymbols(messages.getString("dialogue-not-found", "&cDialogue '{id}' not found."), "plugin-messages.dialogue-not-found");
        msgCustomLineLimit = validateCodesAndSymbols(messages.getString("custom-line-limit", "&cMax {limit} lines allowed."), "plugin-messages.custom-line-limit");
        msgReloadSuccess = validateCodesAndSymbols(messages.getString("reload-success", "&aConfig reloaded."), "plugin-messages.reload-success");
        msgReloadFail = validateCodesAndSymbols(messages.getString("reload-fail", "&cReload failed."), "plugin-messages.reload-fail");
        msgMustBeOnGround = validateCodesAndSymbols(messages.getString("must-be-on-ground", "&cYou must be standing on the ground!"), "plugin-messages.must-be-on-ground");
    }

    private void loadDialogues() {
        loadedDialogues.clear();
        ConfigurationSection dialoguesSection = config.getConfigurationSection("dialogues");
        if (dialoguesSection == null) {
            plugin.getLogger().warning("No 'dialogues' section found in config.yml!");
            return;
        }

        for (String dialogueId : dialoguesSection.getKeys(false)) {
            ConfigurationSection diaSection = dialoguesSection.getConfigurationSection(dialogueId);
            if (diaSection == null) continue;

            ConfigurationSection settingsSection = diaSection.getConfigurationSection("settings");
            String specificBackgroundLine = null;
            Long delay = null;
            BarColor color = null;
            BarStyle style = null;
            String typeSound = null; Float typeVolume = null; Float typePitch = null;
            String startSound = null; Float startVolume = null; Float startPitch = null;

            if (settingsSection != null) {
                if (settingsSection.isSet("persistent-background-line")) {
                    specificBackgroundLine = validateCodesAndSymbols(settingsSection.getString("persistent-background-line"), diaSection.getCurrentPath() + ".settings.persistent-background-line");
                }
                if (settingsSection.isSet("animation-delay-ticks")) delay = Math.max(1L, settingsSection.getLong("animation-delay-ticks"));
                color = safeGetEnum(settingsSection, "bar-color", BarColor.class, null);
                style = safeGetEnum(settingsSection, "bar-style", BarStyle.class, null);

                if (settingsSection.isConfigurationSection("typing-sound")) {
                    typeSound = settingsSection.getString("typing-sound.sound", null);
                    if (settingsSection.isSet("typing-sound.volume")) typeVolume = (float) settingsSection.getDouble("typing-sound.volume");
                    if (settingsSection.isSet("typing-sound.pitch")) typePitch = (float) settingsSection.getDouble("typing-sound.pitch");
                } else if (settingsSection.isSet("typing-sound")) {
                    typeSound = settingsSection.getString("typing-sound");
                }
                if(typeSound != null && typeSound.isBlank()) typeSound = null;

                if (settingsSection.isConfigurationSection("start-sound")) {
                    startSound = settingsSection.getString("start-sound.sound", null);
                    if (settingsSection.isSet("start-sound.volume")) startVolume = (float) settingsSection.getDouble("start-sound.volume");
                    if (settingsSection.isSet("start-sound.pitch")) startPitch = (float) settingsSection.getDouble("start-sound.pitch");
                } else if (settingsSection.isSet("start-sound")) {
                    startSound = settingsSection.getString("start-sound");
                }
                if(startSound != null && startSound.isBlank()) startSound = null;
            }

            Dialogue.DialogueSettings specificSettings = Dialogue.DialogueSettings.create(
                    specificBackgroundLine, delay, color, style,
                    typeSound, typeVolume, typePitch,
                    startSound, startVolume, startPitch
            );

            List<DialoguePage> pages = new ArrayList<>();
            if (diaSection.isList("pages")) {
                List<?> rawPageList = diaSection.getList("pages");
                if (rawPageList != null) {
                    for (int pageIndex = 0; pageIndex < rawPageList.size(); pageIndex++) {
                        Object pageObj = rawPageList.get(pageIndex);
                        if (pageObj instanceof List) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<String> rawLines = ((List<?>)pageObj).stream().map(Object::toString).collect(Collectors.toList());
                                List<String> validatedLines = new ArrayList<>();
                                for(int lineIndex = 0; lineIndex < rawLines.size(); lineIndex++) {
                                    String line = rawLines.get(lineIndex);
                                    String validatedLine = validateCodesAndSymbols(line, diaSection.getCurrentPath() + ".pages." + pageIndex + "." + lineIndex);
                                    validatedLines.add(validatedLine);
                                }
                                if (!validatedLines.isEmpty()) pages.add(new DialoguePage(validatedLines));
                            } catch (ClassCastException e) { plugin.getLogger().warning("Invalid page structure in dialogue '" + dialogueId + "'."); }
                        } else { plugin.getLogger().warning("Invalid page structure in dialogue '" + dialogueId + "'. Found non-list item."); }
                    }
                }
            } else { plugin.getLogger().warning("Missing/invalid 'pages' list in dialogue '" + dialogueId + "'."); }

            List<String> actionStrings = diaSection.getStringList("end-actions");
            List<DialogueAction> endActions = actionStrings.stream()
                    .map(DialogueAction::parse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Dialogue dialogue = new Dialogue(dialogueId, pages, specificSettings, endActions);
            if (dialogue.isValid()) {
                loadedDialogues.put(dialogueId.toLowerCase(), dialogue);
            } else { plugin.getLogger().warning("Dialogue '" + dialogueId + "' invalid (likely no pages) and was not loaded."); }
        }
    }

    private <T extends Enum<T>> T safeGetEnum(ConfigurationSection s, String p, Class<T> c, T d) {
        if(!s.isSet(p)){return d;} String v=s.getString(p);
        if(v==null||v.isBlank()){return d;}
        try{return Enum.valueOf(c,v.toUpperCase().replace('-','_'));}
        catch(IllegalArgumentException e){plugin.getLogger().warning("Invalid "+c.getSimpleName()+" value '"+v+"' at path '"+s.getCurrentPath()+"."+p+"'. Using default: "+d); return d;}
    }

    public String getGlobalPersistentBackgroundLine() { return globalPersistentBackgroundLine; }
    public long getDefaultAnimationDelayTicks() { return defaultAnimationDelayTicks; }
    public BarColor getDefaultBarColor() { return defaultBarColor; }
    public BarStyle getDefaultBarStyle() { return defaultBarStyle; }
    public Optional<String> getDefaultTypingSound() { return Optional.ofNullable(defaultTypingSound); }
    public float getDefaultTypingVolume() { return defaultTypingVolume; }
    public float getDefaultTypingPitch() { return defaultTypingPitch; }
    public long getDefaultHoldTimeTicks() { return defaultHoldTimeTicks; }
    public boolean isMovementRestrictionEnabled() { return movementRestrictionEnabled; }
    public boolean isLockPerspectiveEnabled() { return lockPerspectiveEnabled; }
    public boolean isPapiEnabled() { return papiEnabled; }
    public boolean isDebugLogging() { return debugLogging; }
    public int getCustomCommandMaxLines() { return customCommandMaxLines; }
    public String getCustomCommandBackgroundChar() { return customCommandBackgroundChar; }
    public Optional<String> getDefaultStartSound() { return Optional.ofNullable(defaultStartSound); }
    public float getDefaultStartVolume() { return defaultStartVolume; }
    public float getDefaultStartPitch() { return defaultStartPitch; }
    public long getEndActionDelayTicks() { return endActionDelayTicks; }

    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgPlayerOnly() { return msgPlayerOnly; }
    public String getMsgDialogueNotFound(String id) { return msgDialogueNotFound.replace("{id}", id); }
    public String getMsgCustomLineLimit() { return msgCustomLineLimit.replace("{limit}", String.valueOf(customCommandMaxLines)); }
    public String getMsgReloadSuccess() { return msgReloadSuccess; }
    public String getMsgReloadFail() { return msgReloadFail; }
    public String getMsgMustBeOnGround() { return msgMustBeOnGround; }

    public Optional<Dialogue> getDialogue(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(loadedDialogues.get(id.toLowerCase()));
    }

    public Set<String> getLoadedDialogueIds() {
        return loadedDialogues.keySet();
    }

    public StrikesDialogues getPlugin() {
        return plugin;
    }
}