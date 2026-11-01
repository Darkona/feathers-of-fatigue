package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.mount.MountExertion;
import com.darkona.feathersoffatigue.content.mount.MountTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.assertValueEqual;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;

@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class MountTests {
    private static Horse horse(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, new BlockPos(1, 2, 1));
        horse.setTamed(true);
        horse.equipSaddle(SoundSource.NEUTRAL);
        return horse;
    }

    @GameTest(template = "empty")
    public static void horsesHaveTheirOwnStamina(GameTestHelper helper) {
        Horse horse = horse(helper);
        FeathersView feathers = FeathersAPI.get(horse);
        helper.assertTrue(FeathersAPI.hasFeathers(horse), "a horse has feathers");
        int max = feathers.maxFeathers();
        helper.assertTrue(max >= 14 && max <= 30, "the rolled stamina is within the configured range: " + max);
        assertValueEqual(helper, feathers.stamina(), feathers.maxStamina(), "a new horse is full");

        MountTraits.ensureRolled(horse);
        assertValueEqual(helper, FeathersAPI.get(horse).maxFeathers(), max, "the trait is rolled only once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foalsTakeAfterTheirParents(GameTestHelper helper) {
        Horse mare = horse(helper);
        Horse stallion = horse(helper);
        mare.getAttribute(FeathersAttributes.MAX_FEATHERS.get()).setBaseValue(30);
        stallion.getAttribute(FeathersAttributes.MAX_FEATHERS.get()).setBaseValue(28);
        Horse foal = helper.spawn(EntityType.HORSE, new BlockPos(1, 2, 1));

        MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(mare, stallion, foal));
        double trait = foal.getAttribute(FeathersAttributes.MAX_FEATHERS.get()).getBaseValue();

        helper.assertTrue(trait >= 26 && trait <= 30, "the foal takes after strong parents: " + trait);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void jumpsCostByPower(GameTestHelper helper) {
        Horse horse = horse(helper);

        int before = FeathersAPI.get(horse).stamina();
        MountExertion.chargeJump(horse, player(helper), 50);
        int expected = Stamina.ofFeathers(ServerConfig.MOUNT_JUMP_FEATHERS.get() * 0.5);
        assertValueEqual(helper, before - FeathersAPI.get(horse).stamina(), expected, "a half jump costs half a full jump");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exhaustedHorsesSlowDownAndCantJump(GameTestHelper helper) {
        Horse horse = horse(helper);
        FeathersView feathers = FeathersAPI.get(horse);
        FeathersAPI.spend(horse, id("test"), feathers.maxStamina() + feathers.maxStrain());
        helper.assertTrue(FeathersAPI.get(horse).exhausted(), "spent everything, strain included");

        MountExertion.tickMount(horse);
        helper.assertTrue(MountExertion.isSlowedDown(horse), "an exhausted horse slows down");
        helper.assertFalse(horse.canJump(), "an exhausted horse can't jump");

        FeathersAPI.reset(horse);
        MountExertion.tickMount(horse);
        helper.assertFalse(MountExertion.isSlowedDown(horse), "a rested horse runs again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exMountsAreReleased(GameTestHelper helper) {
        Horse horse = horse(helper);
        FeathersView feathers = FeathersAPI.get(horse);
        FeathersAPI.spend(horse, id("test"), feathers.maxStamina() + feathers.maxStrain());
        MountExertion.tickMount(horse);
        helper.assertTrue(horse.hasEffect(FeathersMobEffects.STRAINED.get()), "a strained horse shows it");
        helper.assertTrue(MountExertion.isSlowedDown(horse), "an exhausted horse slows down");

        MountExertion.release(horse);
        helper.assertFalse(horse.hasEffect(FeathersMobEffects.STRAINED.get()), "the Strained effect goes");
        helper.assertFalse(MountExertion.isSlowedDown(horse), "the slowdown goes");
        feathers = FeathersAPI.get(horse);
        assertValueEqual(helper, feathers.strain(), 0, "no strain left");
        helper.assertFalse(feathers.exhausted(), "no exhaustion left");

        horse.addEffect(new MobEffectInstance(FeathersMobEffects.STRAINED.get(), 200));
        MountExertion.release(horse);
        helper.assertTrue(horse.hasEffect(FeathersMobEffects.STRAINED.get()), "someone else's Strained effect stays");

        horse.removeEffect(FeathersMobEffects.STRAINED.get());
        FeathersAPI.spend(horse, id("test"), FeathersAPI.get(horse).availableStamina() + FeathersAPI.get(horse).maxStrain());
        MountExertion.tickMount(horse);
        helper.assertTrue(MountExertion.isSlowedDown(horse), "an exhausted mount slows down again");

        Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 2, 1));
        MountExertion.release(cow);
        helper.assertTrue(FeathersServiceImpl.dataOrNull(cow) == null, "a cow gets no feathers from the release");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dataMakesAnyCreatureAMount(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        helper.assertTrue(FeathersAPI.hasFeathers(pig), "the data map makes a pig a mount");
        assertValueEqual(helper, FeathersAPI.get(pig).maxFeathers(), 40, "its stamina comes from its stats");
        assertValueEqual(helper, FeathersAPI.getRegenPerSecond(pig), 2.0, "and so does its regeneration");
        helper.assertFalse(FeathersAPI.hasFeathers(helper.spawn(EntityType.COW, new BlockPos(1, 2, 1))), "cows stay out");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void optedOutCreaturesHaveNoFeathers(GameTestHelper helper) {
        helper.assertFalse(FeathersAPI.hasFeathers(helper.spawn(EntityType.DONKEY, new BlockPos(1, 2, 1))), "donkeys opted out");
        helper.assertTrue(FeathersAPI.hasFeathers(helper.spawn(EntityType.MULE, new BlockPos(1, 2, 1))), "mules still tire");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void otherModsMountsHaveFeathers(GameTestHelper helper) {
        checkSpawned(helper, "naturalist", "naturalist:ostrich");
        checkSpawned(helper, "mr_mob_wrangler", "minecraft:sniffer");
        helper.succeed();
    }

    private static void checkSpawned(GameTestHelper helper, String mod, String type) {
        if (!ModList.get().isLoaded(mod)) return;
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(type));
        if (helper.spawn(entityType, new BlockPos(1, 2, 1)) instanceof LivingEntity living) {
            helper.assertTrue(FeathersAPI.hasFeathers(living), type + " has feathers with " + mod);
        }
    }
}
