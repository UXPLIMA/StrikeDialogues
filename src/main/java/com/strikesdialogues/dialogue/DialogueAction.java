package com.strikesdialogues.dialogue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class DialogueAction {


    private static final Pattern ACTION_PATTERN = Pattern.compile("^\\[(\\w+)\\]\\s*(.*)$");
    private final ActionType type;
    private final String argument;

    public DialogueAction(ActionType type, String argument) {
        this.type = type;
        this.argument = argument;
    }

    public ActionType getType() {
        return type;
    }

    public String getArgument() {
        return argument;
    }

    public static DialogueAction parse(String configString) {
        if (configString == null || configString.isBlank()) {
            return null;
        }
        Matcher matcher = ACTION_PATTERN.matcher(configString.trim());
        if (matcher.matches()) {
            String typeStr = matcher.group(1).toUpperCase();
            String argument = matcher.group(2);

            try {
                ActionType type = ActionType.valueOf(typeStr);
                return new DialogueAction(type, argument);
            } catch (IllegalArgumentException e) {

                System.err.println("[StrikesDialogues] WARN: Unknown action type '" + typeStr + "' in config string: " + configString);
                return null;

            }
        }


        System.err.println("[StrikesDialogues] WARN: Could not parse action string format: " + configString + ". Assuming MESSAGE action.");

        return new DialogueAction(ActionType.MESSAGE, configString);



    }

    public enum ActionType {
        CONSOLE,
        PLAYER,
        MESSAGE,
        BROADCAST,
        SOUND


    }

    @Override
    public String toString() {
        return "DialogueAction{" +
                "type=" + type +
                ", argument='" + argument + '\'' +
                '}';
    }
}