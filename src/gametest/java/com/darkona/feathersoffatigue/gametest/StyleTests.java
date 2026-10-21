package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.client.FeatherStyle;
import com.darkona.feathersoffatigue.api.client.FeatherStyles;
import com.darkona.feathersoffatigue.api.client.FeatherVariants;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.style.FeatherStylePack;
import com.darkona.feathersoffatigue.style.FeatherStylePack.Patch;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;

/**
 * The feather style API: registry, providers and resource packs' feather_styles.json. None of it touches client
 * classes, so it runs here. Registrations are global and permanent, so providers only answer for tagged players.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class StyleTests {

    private static final String MARK = "feathers_of_fatigue_style_test";
    private static final String HIGH_MARK = "feathers_of_fatigue_style_test_high";
    private static final ResourceLocation LOW = id("test_style_low");
    private static final ResourceLocation HIGH = id("test_style_high");

    static {
        FeatherStyles.registerStyle(LOW, FeatherStyle.opaque(0x112233, 0x000000));
        FeatherStyles.registerStyle(HIGH, FeatherStyle.opaque(0x445566, 0xFFFFFF));
        FeatherStyles.registerStyleProvider(id("test_style_low"), 500, (player, feathers) -> player.getTags().contains(MARK) ? LOW : null);
        FeatherStyles.registerStyleProvider(id("test_style_high"), 1000, (player, feathers) -> player.getTags().contains(HIGH_MARK) ? HIGH : null);
    }

    @GameTest(template = "empty")
    public static void feathersOfFatigueRegistersItsStyles(GameTestHelper helper) {
        for (ResourceLocation id : new ResourceLocation[]{FeatherStyles.GREEN, FeatherStyles.BLUE, FeatherStyles.WHITE, FeatherStyles.COLD,
                FeatherStyles.HOT, FeatherStyles.ENERGIZED, FeatherStyles.MOMENTUM, FeatherStyles.STRAIN, FeatherStyles.ENDURANCE,
                FeatherStyles.ARMOR, FeatherStyles.EMPTY, FeatherStyles.EXHAUSTED}) {
            helper.assertTrue(FeatherStyles.get(id) != null, id + " is registered");
        }
        helper.assertValueEqual(FeatherStyles.get(FeatherStyles.BLUE), new FeatherStyle(0xFF22A5F0, 0xFF000000), "Elenai's blue, outlined in black");
        helper.assertValueEqual(FeatherStyles.get(FeatherStyles.ARMOR).border(), FeatherStyle.NO_BORDER, "unattributed weight has no outline");
        helper.assertValueEqual(FeatherStyles.get(FeatherStyles.COLD).variant(), FeatherVariants.CRYSTAL, "cold keeps its crystal feathers");
        helper.assertValueEqual(FeatherStyles.get(FeatherStyles.COLD).overlay(), FeatherVariants.FROST, "and frost over them");
        helper.assertTrue(FeatherStyles.get(FeatherStyles.HOT).overlay() == null, "heat keeps the plain orange feather, no flames");
        helper.assertTrue(FeatherVariants.overlay(FeatherVariants.HEAT) != null, "the flames overlay stays registered for styles and packs");
        for (ResourceLocation id : new ResourceLocation[]{FeatherVariants.FEATHER, FeatherVariants.CRYSTAL, FeatherVariants.GLINT,
                FeatherVariants.STRAINED, FeatherVariants.PLAIN}) {
            helper.assertTrue(FeatherVariants.variant(id) != null, id + " is a registered variant");
        }
        helper.assertValueEqual(FeatherVariants.overlay(FeatherVariants.FROST), new FeatherVariants.Sprites(FeatherVariants.SHEET, 6, 56, 72), "frost in row 6 of the 56x72 sheet");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sheetSizesAreChecked(GameTestHelper helper) {
        ResourceLocation texture = id("textures/gui/test.png");
        FeatherVariants.registerVariant(id("test_variant_sized"), texture, 6, 64, 64);
        helper.assertValueEqual(FeatherVariants.variant(id("test_variant_sized")), new FeatherVariants.Sprites(texture, 6, 64, 64), "a 64x64 texture keeps its size");
        helper.assertTrue(rejects(() -> FeatherVariants.registerVariant(id("test_variant_bad"), texture, 0, 60, 72)), "a width that is not a multiple of 8");
        helper.assertTrue(rejects(() -> FeatherVariants.registerVariant(id("test_variant_bad"), texture, 8)), "a row below the sheet");
        helper.assertTrue(rejects(() -> FeatherVariants.registerVariant(id("test_variant_bad"), texture, 0, 16, 16)), "a texture too narrow for six cells");
        FeatherVariants.registerOverlay(id("test_overlay_sized"), texture, 1, 24, 24);
        helper.assertTrue(rejects(() -> FeatherVariants.registerOverlay(id("test_overlay_bad"), texture, 0, 24, 20)), "a height that is not a multiple of 8");
        helper.assertTrue(FeatherVariants.variant(id("test_variant_bad")) == null && FeatherVariants.overlay(id("test_overlay_bad")) == null, "nothing registered when rejected");
        helper.succeed();
    }

    private static boolean rejects(Runnable registration) {
        try {
            registration.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    @GameTest(template = "empty")
    public static void registeringAgainReplaces(GameTestHelper helper) {
        ResourceLocation id = id("test_style_replaced");
        FeatherStyles.registerStyle(id, FeatherStyle.opaque(0x000001, 0));
        int version = FeatherStyles.version();
        FeatherStyle replacement = new FeatherStyle(0x80FFFFFF, FeatherStyle.NO_BORDER).withSprites(id("textures/gui/test.png"));
        FeatherStyles.registerStyle(id, replacement);
        helper.assertValueEqual(FeatherStyles.get(id), replacement, "the later registration");
        helper.assertTrue(FeatherStyles.version() != version, "caches see the change");
        helper.assertValueEqual(FeatherStylePack.resolve(id), replacement, "resolved without a resource pack change");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void higherPriorityStyleProvidersWin(GameTestHelper helper) {
        ServerPlayer low = player(helper);
        low.addTag(MARK);
        helper.assertValueEqual(FeatherStyles.select(low, FeathersAPI.get(low)), LOW, "the only provider answering");
        ServerPlayer both = player(helper);
        both.addTag(MARK);
        both.addTag(HIGH_MARK);
        helper.assertValueEqual(FeatherStyles.select(both, FeathersAPI.get(both)), HIGH, "the higher priority");
        // Above Feathers of Fatigue's own states: a cold marked player still gets the test style.
        low.addEffect(new MobEffectInstance(FeathersMobEffects.COLD, 200));
        helper.assertValueEqual(FeatherStyles.select(low, FeathersAPI.get(low)), LOW, "over the cold state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void feathersOfFatigueStatesAnswerThroughTheProviders(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(FeatherStyles.select(player, FeathersAPI.get(player)) == null, "no state: the configured color");
        player.addEffect(new MobEffectInstance(FeathersMobEffects.MOMENTUM, 200));
        helper.assertValueEqual(FeatherStyles.select(player, FeathersAPI.get(player)), FeatherStyles.MOMENTUM, "momentum");
        player.addEffect(new MobEffectInstance(FeathersMobEffects.ENERGIZED, 200));
        helper.assertValueEqual(FeatherStyles.select(player, FeathersAPI.get(player)), FeatherStyles.ENERGIZED, "energized before momentum");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void styleColorsParse(GameTestHelper helper) {
        helper.assertValueEqual(FeatherStyle.parseColor("#22A5F0").getOrThrow(), 0xFF22A5F0, "#RRGGBB is opaque");
        helper.assertValueEqual(FeatherStyle.parseColor("#8022a5f0").getOrThrow(), 0x8022A5F0, "#AARRGGBB keeps its alpha");
        helper.assertTrue(FeatherStyle.parseColor("22A5F0").isError(), "the # is required");
        helper.assertTrue(FeatherStyle.parseColor("#22A5F").isError(), "six or eight digits");
        helper.assertTrue(FeatherStyle.parseColor("#GGA5F0").isError(), "hex digits");
        FeatherStyle style = new FeatherStyle(0x8022A5F0, 0xFF000000).withVariant(FeatherVariants.CRYSTAL)
                .withOverlay(FeatherVariants.FROST, 0xFF8DC8FE, 0x80FFFFFF).withSprites(id("textures/gui/test.png"));
        helper.assertValueEqual(FeatherStyle.CODEC.parse(JsonOps.INSTANCE, FeatherStyle.CODEC.encodeStart(JsonOps.INSTANCE, style).getOrThrow()).getOrThrow(),
                style, "the codec round-trips");
        helper.assertValueEqual(FeatherStyle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"body\": \"#22A5F0\"}")).getOrThrow(),
                FeatherStyle.opaque(0x22A5F0, 0x000000), "only the body is required: black border, plain feather, no overlay");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void resourcePackFilesRecolorStyles(GameTestHelper helper) {
        ResourceLocation partial = id("test_style_partial");
        ResourceLocation added = id("test_style_added");
        ResourceLocation bodyless = id("test_style_bodyless");
        FeatherStyle registered = FeatherStyle.opaque(0x123456, 0x654321).withVariant(FeatherVariants.CRYSTAL).withOverlay(FeatherVariants.FROST, -1, -1);
        FeatherStyles.registerStyle(partial, registered);
        Map<ResourceLocation, Patch> lower = FeatherStylePack.parse(JsonParser.parseString("""
                {"styles": {
                  "feathers_of_fatigue:test_style_partial": {"border": "#FF0000", "sprites": "feathers_of_fatigue:textures/gui/lower.png", "overlay": "none"},
                  "feathers_of_fatigue:test_style_added": {"body": "#00FF00"},
                  "feathers_of_fatigue:test_style_bodyless": {"border": "#00FF00"},
                  "feathers_of_fatigue:test_style_broken": {"body": "green"},
                  "Not An Id": {"body": "#000000"}
                }}"""), "lower");
        helper.assertValueEqual(lower.size(), 3, "broken entries are skipped, the rest kept");
        Map<ResourceLocation, Patch> higher = FeatherStylePack.parse(JsonParser.parseString("""
                {"styles": {"feathers_of_fatigue:test_style_partial": {"sprites": "feathers_of_fatigue:textures/gui/higher.png", "variant": "feathers_of_fatigue:glint"},
                            "feathers_of_fatigue:test_style_added": {"overlay": "feathers_of_fatigue:heat", "overlay_accent": "#80FF0000"}}}"""), "higher");
        helper.assertValueEqual(FeatherStylePack.parse(JsonParser.parseString("[]"), "not an object").size(), 0, "no styles object, nothing");

        Map<ResourceLocation, Patch> merged = new HashMap<>();
        FeatherStylePack.merge(merged, lower);
        FeatherStylePack.merge(merged, higher);
        FeatherStylePack.setPatches(merged);
        try {
            helper.assertValueEqual(FeatherStylePack.resolve(partial), FeatherStyle.opaque(0x123456, 0xFF0000).withVariant(FeatherVariants.GLINT)
                    .withSprites(id("textures/gui/higher.png")), "registered body, lower pack's border and no overlay, higher pack's variant and sprites");
            helper.assertValueEqual(FeatherStylePack.resolve(added), FeatherStyle.opaque(0x00FF00, 0x000000)
                    .withOverlay(FeatherVariants.HEAT, FeatherStyle.WHITE, 0x80FF0000), "a new style: black border, the packs' overlay");
            helper.assertTrue(FeatherStylePack.resolve(bodyless) == null, "a new style needs a body");
            helper.assertValueEqual(FeatherStyles.get(partial), registered, "the registry keeps what code registered");
        } finally {
            FeatherStylePack.setPatches(Map.of());
        }
        helper.assertValueEqual(FeatherStylePack.resolve(partial), registered, "packs gone, the registered style");
        helper.succeed();
    }
}
