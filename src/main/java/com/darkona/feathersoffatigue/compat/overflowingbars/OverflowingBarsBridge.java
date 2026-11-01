package com.darkona.feathersoffatigue.compat.overflowingbars;

import fuzs.overflowingbars.client.handler.RowCountRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class OverflowingBarsBridge {

    private OverflowingBarsBridge() {}

    public static void drawRowCount(GuiGraphics graphics, int x, int y, int layers, Font font) {
        RowCountRenderer.drawBarRowCount(graphics, x, y, layers, true, 1, font);
    }
}
