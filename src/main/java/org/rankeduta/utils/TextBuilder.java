package org.rankeduta.utils;

import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextBuilder {
    private final List<Text> parts = new ArrayList<>();

    public TextBuilder append(Text text) {
        parts.add(text.copy());
        return this;
    }

    public TextBuilder append(String text) {
        parts.add(Text.literal(text).setStyle(Style.EMPTY));
        return this;
    }

    public Text build() {
        if (parts.isEmpty()) return Text.empty();
        Text result = parts.getFirst();
        for (int i = 1; i < parts.size(); i++) {
            result = result.copy().append(parts.get(i));
        }
        return result;
    }
}
