package xyz.rimvisuals.gui;

import java.util.function.Predicate;

/** Minimal text field state; drawing and focus are handled by the screen. */
public final class TextInput {
    public String text = "";
    public final String placeholder;
    public final int max;
    public Predicate<Character> filter;
    public Runnable onChange;

    public TextInput(String placeholder, int max) { this.placeholder = placeholder; this.max = max; }

    public boolean type(char ch) {
        if (ch < 32 || ch == 127) return false;
        if (filter != null && !filter.test(ch)) return true;
        if (text.length() < max) { text += ch; if (onChange != null) onChange.run(); }
        return true;
    }

    public void backspace() {
        if (text.isEmpty()) return;
        text = text.substring(0, text.length() - 1);
        if (onChange != null) onChange.run();
    }
}
