package org.rankeduta.utils;

import net.minecraft.text.MutableText;
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
        MutableText result = Text.empty();
	    for (Text part : parts)
		    result.append(part);
        return result;
    }
}
