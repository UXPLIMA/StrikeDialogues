package com.strikesdialogues.util;

import java.util.HashMap;
import java.util.Map;

public class CharacterMapper {

    private static final Map<Character, Character> charMap = new HashMap<>();
    private static final char UNSUPPORTED_CHAR_PLACEHOLDER = '?';
    private static final String ALLOWED_SYMBOLS_AND_PUNCTUATION = ".,!?'\":;%_#-/[]{}|";

    static {
        charMap.put('A', ''); charMap.put('B', ''); charMap.put('C', '');
        charMap.put('D', ''); charMap.put('E', ''); charMap.put('F', '');
        charMap.put('G', ''); charMap.put('H', ''); charMap.put('I', '');
        charMap.put('J', ''); charMap.put('K', ''); charMap.put('L', '');
        charMap.put('M', ''); charMap.put('N', ''); charMap.put('O', '');
        charMap.put('P', ''); charMap.put('Q', ''); charMap.put('R', '');
        charMap.put('S', ''); charMap.put('T', ''); charMap.put('U', '');
        charMap.put('V', ''); charMap.put('W', ''); charMap.put('X', '');
        charMap.put('Y', ''); charMap.put('Z', '');

        charMap.put('a', ''); charMap.put('b', ''); charMap.put('c', '');
        charMap.put('d', ''); charMap.put('e', ''); charMap.put('f', '');
        charMap.put('g', ''); charMap.put('h', ''); charMap.put('i', '');
        charMap.put('j', ''); charMap.put('k', ''); charMap.put('l', '');
        charMap.put('m', ''); charMap.put('n', ''); charMap.put('o', '');
        charMap.put('p', ''); charMap.put('q', ''); charMap.put('r', '');
        charMap.put('s', ''); charMap.put('t', ''); charMap.put('u', '');
        charMap.put('v', ''); charMap.put('w', ''); charMap.put('x', '');
        charMap.put('y', ''); charMap.put('z', '');

        charMap.put('0', ''); charMap.put('1', ''); charMap.put('2', '');
        charMap.put('3', ''); charMap.put('4', ''); charMap.put('5', '');
        charMap.put('6', ''); charMap.put('7', ''); charMap.put('8', '');
        charMap.put('9', '');

        charMap.put(' ', '');
        charMap.put('!', ''); charMap.put('"', ''); charMap.put('#', '');
        charMap.put('$', ''); charMap.put('%', ''); charMap.put('&', '');
        charMap.put('\'', ''); charMap.put('(', ''); charMap.put(')', '');
        charMap.put('*', ''); charMap.put('+', ''); charMap.put(',', '');
        charMap.put('-', ''); charMap.put('.', ''); charMap.put('/', '');
        charMap.put(':', ''); charMap.put(';', ''); charMap.put('<', '');
        charMap.put('=', ''); charMap.put('>', ''); charMap.put('?', '');
        charMap.put('@', ''); charMap.put('[', ''); charMap.put('\\', '');
        charMap.put(']', ''); charMap.put('^', ''); charMap.put('_', '');
        charMap.put('`', ''); charMap.put('{', ''); charMap.put('|', '');
        charMap.put('}', ''); charMap.put('~', '');

        charMap.put('', '');

    }

    public static String translate(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        int n = inputText.length();
        for (int i = 0; i < n; i++) {
            char currentChar = inputText.charAt(i);

            if (currentChar == '§' && i + 1 < n) {
                char nextChar = inputText.charAt(i + 1);
                if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(nextChar) != -1) {
                    output.append('§').append(nextChar); i++; continue;
                } else if (nextChar == 'x' || nextChar == 'X') {
                    if (i + 13 < n && inputText.charAt(i+2) == '§') {
                        output.append(inputText, i, i + 14); i += 13; continue;
                    }
                    output.append('§').append(nextChar); i++; continue;
                }
                output.append('§'); continue;
            }

            output.append(charMap.getOrDefault(currentChar, UNSUPPORTED_CHAR_PLACEHOLDER));
        }
        return output.toString();
    }

    public static boolean isCharacterSupported(char c) {
        return charMap.containsKey(c)
                || Character.isLetterOrDigit(c)
                || ALLOWED_SYMBOLS_AND_PUNCTUATION.indexOf(c) != -1;
    }
}