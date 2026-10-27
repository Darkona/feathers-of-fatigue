package com.darkona.feathersoffatigue;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.data.DataProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.Mixin;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchTest {

    private static final String MOD = "com.darkona.feathersoffatigue";

    private static final DescribedPredicate<JavaClass> PLATFORM = resideInAnyPackage("java..", "javax..", "jdk..", "net.minecraft..",
            "com.mojang..", "net.neoforged..", "it.unimi.dsi.fastutil..", "org.jetbrains.annotations..", "org.jspecify.annotations..",
            "org.joml..", "org.slf4j..", "com.google..", "io.netty..", "org.apache..", "dev.darkona.jdlib..", "org.spongepowered..",
            "com.llamalad7..", MOD + "..");

    private static final JavaClasses CLASSES = new ClassFileImporter().importPaths(
            Path.of(System.getProperty("feathers.classes")), Path.of(System.getProperty("feathers.gametestClasses")));

    private static final Set<String> CLIENT_CLASSES = CLASSES.stream()
            .filter(type -> type.getPackageName().startsWith(MOD))
            .filter(type -> type.getDirectDependenciesFromSelf().stream()
                    .anyMatch(dependency -> dependency.getTargetClass().getPackageName().startsWith("net.minecraft.client")))
            .map(type -> type.getName().replaceFirst("\\$.*", ""))
            .collect(Collectors.toSet());

    private static final DescribedPredicate<JavaClass> USES_CLIENT = DescribedPredicate.describe("a class that uses net.minecraft.client",
            type -> CLIENT_CLASSES.contains(type.getName().replaceFirst("\\$.*", "")));

    @Test
    void registriesLiveInIndex() {
        noClasses().that().resideOutsideOfPackages(MOD + ".index..", MOD + ".compat..")
                .should().dependOnClassesThat(assignableTo(DeferredRegister.class))
                .check(CLASSES);
    }

    @Test
    void configSpecsLiveInConfig() {
        noClasses().that().resideOutsideOfPackage(MOD + ".config..")
                .should().dependOnClassesThat(assignableTo(ModConfigSpec.class).or(assignableTo(ModConfigSpec.Builder.class)))
                .check(CLASSES);
    }

    @Test
    void dataProvidersLiveInData() {
        noClasses().that().resideOutsideOfPackage(MOD + ".data..")
                .should().dependOnClassesThat(assignableTo(DataProvider.class))
                .check(CLASSES);
    }

    @Test
    void payloadsLiveInNetwork() {
        classes().that().resideInAPackage(MOD + "..").and().areAssignableTo(CustomPacketPayload.class)
                .should().resideInAPackage(MOD + ".network..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void serverCodeNeverNamesClientCode() {
        noClasses().that().resideInAPackage(MOD + "..").and(DescribedPredicate.not(USES_CLIENT))
                .and().doNotHaveFullyQualifiedName(MOD + ".FeathersClient")
                .and().resideOutsideOfPackage(MOD + ".index.client..")
                .should().dependOnClassesThat(USES_CLIENT)
                .check(CLASSES);
    }

    @Test
    void onlyCompatNamesOtherMods() {
        classes().that().resideInAPackage(MOD + "..").and().resideOutsideOfPackages(MOD + ".compat..", MOD + ".gametest.scenario..")
                .should().onlyDependOnClassesThat(PLATFORM)
                .check(CLASSES);
    }

    @Test
    void onlyMixinsInMixin() {
        classes().that().resideInAPackage(MOD + ".mixin..").and().areTopLevelClasses()
                .should().beAnnotatedWith(Mixin.class)
                .check(CLASSES);
    }
}
