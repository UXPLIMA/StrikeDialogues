package com.strikesdialogues.dialogue;

import java.util.Collections;
import java.util.List;
public class DialoguePage {

    private final List<String> rawLines;

    public DialoguePage(List<String> rawLines) {
        this.rawLines = (rawLines != null) ? List.copyOf(rawLines) : Collections.emptyList();
    }
    public List<String> getRawLines() {
        return rawLines;
    }

    public boolean isEmpty() {
        return rawLines.isEmpty();
    }

    @Override
    public String toString() {
        return "DialoguePage{" +
                "rawLines=" + rawLines +
                '}';
    }
}