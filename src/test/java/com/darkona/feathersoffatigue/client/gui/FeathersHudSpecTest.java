package com.darkona.feathersoffatigue.client.gui;

import dev.darkona.jdlib.api.hud.HudSide;
import dev.darkona.jdlib.api.hud.JDHudBars;
import dev.darkona.jdlib.api.hud.VanillaLayer;
import dev.darkona.jdlib.api.hud.StackMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The HUD moved from a vanilla layer to a JDLib bar. The place and the reserved height that come out of the Spec must
 * be the ones the old {@code FeathersHud.render} computed.
 */
class FeathersHudSpecTest {
    private static final int ROW_HEIGHT = 10;
    private static final int[][] WINDOWS = {{427, 240}, {854, 480}, {320, 180}, {1000, 563}, {301, 175}};
    private static final int[] OFFSETS = {-30, -5, 0, 4, 17};
    private static final int[] RIGHT_HEIGHTS = {39, 49, 59, 71};

    /** The old formulas, verbatim. */
    private static int oldX(int guiWidth, int xOffset) {
        return guiWidth / 2 + 91 - 9 + xOffset;
    }

    private static int oldY(int guiHeight, boolean stack, int rightHeight, int yOffset) {
        return guiHeight - (stack ? rightHeight : 49) + yOffset;
    }

    @Test
    void theSpecIsThePlaceOfTheOldLayer() {
        JDHudBars.Spec spec = FeathersHud.spec(() -> true, () -> 0, () -> 0);
        assertEquals(FeathersHud.LAYER_ID, spec.id());
        assertEquals(VanillaLayer.AIR_LEVEL, spec.anchor());
        assertEquals(JDHudBars.Order.ABOVE, spec.order());
        assertEquals(HudSide.RIGHT, spec.side());
        assertEquals(49, spec.unstackedBase());
    }

    @Test
    void positionMatchesTheOldFormulaForEveryCombination() {
        for (boolean stack : new boolean[]{true, false}) {
            for (int dx : OFFSETS) {
                for (int dy : OFFSETS) {
                    boolean s = stack;
                    int xo = dx, yo = dy;
                    JDHudBars.Spec spec = FeathersHud.spec(() -> s, () -> xo, () -> yo);
                    for (int[] w : WINDOWS) {
                        for (int right : RIGHT_HEIGHTS) {
                            int edge = StackMath.edgeX(spec.side(), w[0], spec.xOffset().getAsInt());
                            int height = StackMath.stackHeight(spec.stacks().getAsBoolean(), right, spec.unstackedBase());
                            int top = StackMath.topY(w[1], height, spec.yOffset().getAsInt());
                            String at = "stack=" + s + " off=" + xo + "," + yo + " window=" + w[0] + "x" + w[1] + " right=" + right;
                            assertEquals(oldX(w[0], xo), FeathersHud.rowX(edge), at);
                            assertEquals(oldY(w[1], s, right, yo), top, at);
                        }
                    }
                }
            }
        }
    }

    @Test
    void reservedHeightMatchesTheOldIncrementForEveryBonusRowCount() {
        for (boolean stack : new boolean[]{true, false}) {
            for (int bonusRows = 0; bonusRows <= 6; bonusRows++) {
                // old: if (stack) gui.rightHeight += ROW_HEIGHT * (1 + bonusRows), also while faded out
                int old = stack ? ROW_HEIGHT * (1 + bonusRows) : 0;
                int fromSpec = StackMath.reserved(stack, FeathersHud.reservedHeight(bonusRows));
                assertEquals(old, fromSpec, "stack=" + stack + " bonusRows=" + bonusRows);
            }
        }
    }
}
