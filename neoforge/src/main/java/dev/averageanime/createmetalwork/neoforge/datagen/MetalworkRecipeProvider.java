package dev.averageanime.createmetalwork.neoforge.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.averageanime.createmetalwork.lib.datagen.LoadCondition;
import dev.averageanime.createmetalwork.lib.datagen.RecipeJson;
import dev.averageanime.createmetalwork.lib.datagen.Variant;
import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.neoforge.datagen.Metal.Source;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.averageanime.createmetalwork.neoforge.datagen.Amounts.BLOCK;
import static dev.averageanime.createmetalwork.neoforge.datagen.Amounts.NUGGET;
import static dev.averageanime.createmetalwork.neoforge.datagen.Amounts.UNIT;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.array;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.chanceResult;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.concreteFluidIngredient;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.fluidIngredient;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.fluidResult;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.itemIngredient;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.itemResult;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.recipe;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.tagIngredient;
import static dev.averageanime.createmetalwork.lib.datagen.RecipeJson.tagResult;

public final class MetalworkRecipeProvider implements DataProvider {

    private static final LoadCondition CREATE = LoadCondition.modLoaded("create");

    private static final String ITEM_REGISTRY = "minecraft:item";

    private static LoadCondition hasItems(String tag) {
        return LoadCondition.tagPopulated(ITEM_REGISTRY, tag);
    }

    private static LoadCondition enabled(String... ids) {
        return LoadCondition.enabled(CreateMetalworkCommon.MOD_ID, ids);
    }

    /** The bare name when the id is this mod's, else null -- only those need the gate. */
    private static String ownId(String id) {
        String prefix = CreateMetalworkCommon.MOD_ID + ":";
        return id.startsWith(prefix) ? id.substring(prefix.length()) : null;
    }

    private static List<LoadCondition> base(Metal metal) {
        List<LoadCondition> conditions = new ArrayList<>();
        conditions.add(CREATE);
        return conditions;
    }

    private static final String TFMG = Metals.TFMG;
    private static final String TFMG_STEEL = "tfmg:molten_steel";
    private static final String TFMG_SLAG = "tfmg:molten_slag";

    private static final String METALLURGY_SLAG = "createmetallurgy:molten_slag";

    /**
     * Both mods' molten slag, which MetalworkFluidTagProvider emits. Every recipe here that consumes
     * slag reads this tag, so either mod's slag works wherever the other's does; recipes that make slag
     * still name their own mod's fluid.
     */
    private static final String SLAG_TAG = "c:molten_slag";

    /** The solid forms, from MetalworkTagProvider, read the same way by {@link #slagSolids}. */
    private static final String SLAG_ITEM = "c:slag";
    private static final String SLAG_BLOCK = "c:storage_blocks/slag";

    /** Metallurgy's own melter byproducts: 30 mB of slag off a dirty dust, 45 off a raw or crushed ore. */
    private static final int SLAG_DIRTY = 30;

    private static final int SLAG_RAW = 45;
    private static final String TFMG_GAS = "tfmg:furnace_gas";
    private static final String TFMG_SILICON = "tfmg:liquid_silicon";

    private static final int RAW_AMOUNT = UNIT;

    /**
     * Twice an ingot, which is the yield bonus a crushed ore paid before 3.0 and the reason to crush
     * at all. Safe to exceed {@link Amounts#UNIT} only because nothing turns an ingot back into a
     * crushed ore -- a dust, which an ore hammer does make from an ingot, must stay at UNIT.
     */
    private static final int CRUSHED_AMOUNT = UNIT * 2;

    private static final double WASH_BONUS = 0.5;

    private final PackOutput.PathProvider recipes;

    public MetalworkRecipeProvider(PackOutput output) {
        this.recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @FunctionalInterface
    private interface Emitter {
        void accept(String folder, String name, JsonObject json);
    }

    @FunctionalInterface
    private interface ForeignEmitter {
        void accept(String namespace, String path, JsonObject json);
    }

    private record Form(String tagPrefix, int amount, String suffix) {}

    private static final List<Form> FORMS = List.of(
            new Form("c:nuggets/", NUGGET, "nugget"),
            new Form("c:ingots/", UNIT, "ingot"),
            new Form("c:storage_blocks/", BLOCK, "block"),
            new Form("c:dusts/", UNIT, "dust"),
            new Form("c:raw_materials/", RAW_AMOUNT, "raw"),
            new Form("c:crushed_raw_materials/", CRUSHED_AMOUNT, "crushed_raw"));

    @Override
    public String getName() {
        return "Create: Metalwork recipes";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> written = new ArrayList<>();
        ForeignEmitter foreign = (namespace, path, json) -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
            written.add(DataProvider.saveStable(output, json, recipes.json(id)));
        };
        Emitter emit = (folder, name, json) ->
                foreign.accept(CreateMetalworkCommon.MOD_ID, folder + "/" + name, json);

        for (Metal metal : Metals.ALL) {
            if (metal.standardForms()) melting(emit, metal);
            casting(emit, metal);
            pressCrushed(emit, metal);
        }
        foreignCasts(emit);
        andesite(emit);
        alloys(emit);
        crushing(emit);
        oreDoubling(emit);
        dustChain(emit);
        washCrushed(emit);
        washCreateCompat(emit, foreign);
        washTungsten(emit, foreign);
        dustRoundTrip(emit);
        slagSink(emit);
        slagCasting(foreign);
        slagSolids(foreign);
        tfmgSteel(foreign);
        tfmgSilicon(foreign);
        metallurgyParity(emit);
        metallurgyCrushed(foreign);
        tfmgParity(emit);
        fantasyAlloys(emit);
        collapsedAlloys(emit);
        vatSmelting(emit);
        vatArcFurnace(emit);
        vatElectrolysis(emit);
        vatLeaching(emit);
        vatSlagRecovery(emit);
        bigCannonsParity(emit);

        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }

    private void melting(Emitter emit, Metal metal) {
        for (Form form : FORMS) {
            String tag = form.tagPrefix() + metal.name();
            for (Variant<String> fluid : fluidVariants(metal)) {
                List<LoadCondition> conditions = base(metal);
                conditions.add(hasItems(tag));
                conditions.addAll(fluid.conditions());

                JsonObject json = recipe("create:mixing", conditions);
                json.add("ingredients", array(tagIngredient(tag)));
                json.add("results", array(fluidResult(fluid.value(), form.amount())));
                json.addProperty("heat_requirement", metal.heat().json);

                emit.accept("mixing/melting",
                        "molten_" + metal.name() + "_from_" + form.suffix() + fluid.suffix(), json);
            }
        }
    }

    private void casting(Emitter emit, Metal metal) {
        String fluidTag = "c:molten_" + metal.name();
        for (Variant<Source> source : solidVariants(metal)) {
            cast(emit, metal, fluidTag, source, source.value().ingot(), UNIT, "ingot");
        }
    }

    /**
     * A metal that melts but records no {@link Source}, with the ingot its owner ships.
     *
     * <p>These cannot take a {@code Source} instead: that would also make {@link #casting} emit the
     * cast-back recipes their {@code Metals.ALL} entry deliberately leaves out, and would change what
     * {@link #pressCrushed} and the fan washing emit. Morkite is absent on purpose -- {@code
     * mythicmetals:morkite} is a mineral with an ore and a block and no ingot, so there is nothing to
     * cast it into, and a {@code FOREIGN_INGOTS} entry is no evidence otherwise: that list holds
     * gems too, so recipes keyed on {@code c:ingots/<metal>} resolve.
     */
    private record ForeignCast(String metal, String ingot, String mod) {}

    private static final List<ForeignCast> FOREIGN_CASTS = List.of(
            new ForeignCast("terminite", "betterend:terminite_ingot", Metals.BETTER_END),
            new ForeignCast("thallasium", "betterend:thallasium_ingot", Metals.BETTER_END),
            new ForeignCast("cincinnasite", "betternether:cincinnasite_ingot", Metals.BETTER_NETHER));

    /** Closes the round trip for the three, which otherwise melt with no way back minus Metallurgy. */
    private void foreignCasts(Emitter emit) {
        for (ForeignCast entry : FOREIGN_CASTS) {
            Metal metal = byName(entry.metal());
            for (Variant<String> fluid : fluidVariants(metal)) {
                List<LoadCondition> conditions = base(metal);
                conditions.add(LoadCondition.modLoaded(entry.mod()));
                conditions.addAll(fluid.conditions());

                JsonObject json = recipe("create:compacting", conditions);
                json.add("ingredients", array(fluidIngredient("c:molten_" + entry.metal(), UNIT)));
                json.add("results", array(itemResult(entry.ingot())));

                emit.accept("compacting",
                        entry.metal() + "_ingot_from_molten" + fluid.suffix(), json);
            }
        }
    }

    /**
     * The fluid-free way to an ingot: one crushed ore pressed in a heated basin, one for one. 1.0.11
     * and 2.0.0 both shipped this beside the fluid cast and the two coexist here as they did there --
     * Create picks by machine, and the mixer melt is {@code create:mixing} rather than compacting.
     *
     * <p>A basin holding both a crushed ore and 90 mB of the metal matches this and {@link #cast},
     * which carries no heat requirement; one basin per step, as those versions also required.
     */
    private void pressCrushed(Emitter emit, Metal metal) {
        String tag = "c:crushed_raw_materials/" + metal.name();
        for (Variant<Source> source : solidVariants(metal)) {
            String ingot = source.value().ingot();
            if (ingot == null) continue;

            List<LoadCondition> conditions = base(metal);
            conditions.add(hasItems(tag));
            conditions.addAll(source.conditions());

            JsonObject json = recipe("create:compacting", conditions);
            json.add("ingredients", array(tagIngredient(tag)));
            json.add("results", array(itemResult(ingot)));
            json.addProperty("heat_requirement", metal.heat().json);

            emit.accept("compacting", metal.name() + "_ingot_from_crushed" + source.suffix(), json);
        }
    }

    private void cast(Emitter emit, Metal metal, String fluidTag, Variant<Source> source,
                      String result, int amount, String form) {
        if (result == null) return;

        List<LoadCondition> conditions = base(metal);
        conditions.addAll(source.conditions());

        JsonObject json = recipe("create:compacting", conditions);
        json.add("ingredients", array(fluidIngredient(fluidTag, amount)));
        json.add("results", array(itemResult(result)));

        emit.accept("compacting", metal.name() + "_" + form + "_from_molten" + source.suffix(), json);
    }

    private void andesite(Emitter emit) {
        JsonObject fromBlock = recipe("create:mixing", List.of(CREATE, enabled("molten_andesite")));
        fromBlock.add("ingredients", array(itemIngredient("minecraft:andesite")));
        fromBlock.add("results", array(fluidResult("createmetalwork:molten_andesite", UNIT)));
        fromBlock.addProperty("heat_requirement", "heated");
        emit.accept("mixing/melting", "molten_andesite_from_block", fromBlock);

        List<LoadCondition> crushed = List.of(CREATE, enabled("molten_andesite"));
        JsonObject fromCrushed = recipe("create:mixing", crushed);
        fromCrushed.add("ingredients", array(tagIngredient("c:crushed_raw_materials/andesite")));
        fromCrushed.add("results", array(fluidResult("createmetalwork:molten_andesite", NUGGET)));
        fromCrushed.addProperty("heat_requirement", "heated");
        emit.accept("mixing/melting", "molten_andesite_from_crushed_raw", fromCrushed);

        // Milling turns one crushed andesite into one dust, and nine crushed make a block, so the dust
        // is worth a nugget here just as the crushed form is. A unit would be a ninefold duplication.
        JsonObject fromDust = recipe("create:mixing",
                List.of(CREATE, hasItems("c:dusts/andesite"), enabled("molten_andesite")));
        fromDust.add("ingredients", array(tagIngredient("c:dusts/andesite")));
        fromDust.add("results", array(fluidResult("createmetalwork:molten_andesite", NUGGET)));
        fromDust.addProperty("heat_requirement", "heated");
        emit.accept("mixing/melting", "molten_andesite_from_dust", fromDust);

        JsonObject castAndesite = recipe("create:compacting", List.of(CREATE));
        castAndesite.add("ingredients", array(fluidIngredient("c:molten_andesite", UNIT)));
        castAndesite.add("results", array(itemResult("minecraft:andesite")));
        emit.accept("compacting", "andesite_from_molten", castAndesite);

        List<LoadCondition> nuggets = List.of(CREATE);
        JsonObject solidAlloy = recipe("create:compacting", nuggets);
        solidAlloy.add("ingredients", array(
                itemIngredient("minecraft:andesite"), tagIngredient("c:andesite_alloy_nuggets")));
        solidAlloy.add("results", array(itemResult("create:andesite_alloy")));
        solidAlloy.addProperty("heat_requirement", "heated");
        emit.accept("compacting", "andesite_alloy_from_andesite_and_nugget", solidAlloy);
    }

    private void alloys(Emitter emit) {
        // Create mixes a copper and a zinc into one brass; doubled like every other alloy.
        alloy(emit, "molten_brass_from_molten", "brass", Metal.Heat.HEATED, UNIT * 2,
                List.of(fluidIngredient("c:molten_copper", UNIT), fluidIngredient("c:molten_zinc", UNIT)),
                List.of());

        // Already doubled, because its output is CRUSHED_AMOUNT rather than a figure of its own. Do not
        // double it again: two crushed ores would then pay four brass.
        alloy(emit, "molten_brass_from_crushed_raw", "brass", Metal.Heat.HEATED, CRUSHED_AMOUNT,
                List.of(tagIngredient("c:crushed_raw_materials/copper"),
                        tagIngredient("c:crushed_raw_materials/zinc")),
                List.of());

        for (String metal : List.of("iron", "zinc")) {
            // Create presses an andesite and a nugget into one andesite alloy. This is the pair 1.0.11
            // paid double on, which is what the doubling restores.
            alloy(emit, "molten_andesite_alloy_from_molten_" + metal, "andesite_alloy",
                    Metal.Heat.HEATED, UNIT * 2,
                    List.of(fluidIngredient("c:molten_andesite", UNIT),
                            fluidIngredient("c:molten_" + metal, NUGGET)),
                    List.of());
        }

        // Vanilla smiths four gold and four scrap into one netherite ingot.
        alloy(emit, "molten_netherite_from_molten_gold", "netherite", Metal.Heat.HEATED, UNIT * 2,
                List.of(fluidIngredient("c:molten_gold", UNIT * 4),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap"),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap"),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap"),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap")),
                List.of());

        // Scrap is not netherite, so its dust gets no melt of its own; it feeds the same alloy the
        // crushed form does, at the same four-to-an-ingot rate.
        alloy(emit, "molten_netherite_from_molten_gold_and_dust", "netherite", Metal.Heat.HEATED,
                UNIT * 2,
                List.of(fluidIngredient("c:molten_gold", UNIT * 4),
                        tagIngredient("c:dusts/netherite_scrap"),
                        tagIngredient("c:dusts/netherite_scrap"),
                        tagIngredient("c:dusts/netherite_scrap"),
                        tagIngredient("c:dusts/netherite_scrap")),
                List.of(hasItems("c:dusts/netherite_scrap")));

        alloy(emit, "molten_steel_from_molten_iron", "steel", Metal.Heat.HEATED, UNIT * 2,
                List.of(fluidIngredient("c:molten_iron", UNIT), tagIngredient("minecraft:coals")),
                List.of());

        // Iron and coal again, which is also what steel is made of just above. Big Cannons separates its
        // own two by machine rather than by ingredient -- compacting presses iron into cast iron, mixing
        // stirs it into steel -- so this follows suit. As create:mixing it would be an ambiguous basin.
        mix(emit, "create:compacting", "compacting/alloying", "molten_cast_iron_from_molten_iron",
                "cast_iron", Metal.Heat.HEATED, UNIT * 2,
                List.of(fluidIngredient("c:molten_iron", UNIT), tagIngredient("minecraft:coals")),
                List.of());
    }

    private void alloy(Emitter emit, String name, String metalName, Metal.Heat heat, int amount,
                       List<JsonObject> ingredients, List<LoadCondition> extra) {
        mix(emit, "mixing/alloying", name, metalName, heat, amount, ingredients, extra);
    }

    private void mix(Emitter emit, String folder, String name, String metalName, Metal.Heat heat,
                     int amount, List<JsonObject> ingredients, List<LoadCondition> extra) {
        mix(emit, "create:mixing", folder, name, metalName, heat, amount, ingredients, extra);
    }

    private void mix(Emitter emit, String type, String folder, String name, String metalName,
                     Metal.Heat heat, int amount, List<JsonObject> ingredients,
                     List<LoadCondition> extra) {
        Metal metal = byName(metalName);
        for (Variant<String> fluid : fluidVariants(metal)) {
            List<LoadCondition> conditions = base(metal);
            conditions.addAll(extra);
            conditions.addAll(fluid.conditions());

            JsonObject json = recipe(type, conditions);
            json.add("ingredients", array(ingredients));
            json.add("results", array(fluidResult(fluid.value(), amount)));
            json.addProperty("heat_requirement", heat.json);

            emit.accept(folder, name + fluid.suffix(), json);
        }
    }

    private void crushing(Emitter emit) {
        JsonObject andesite = recipe("create:crushing", List.of(CREATE, enabled("crushed_andesite")));
        andesite.add("ingredients", array(itemIngredient("minecraft:andesite")));
        andesite.add("results", array(
                itemResult("createmetalwork:crushed_andesite", 9),
                chanceResult("createmetalwork:crushed_andesite", 0.1),
                chanceResult("create:experience_nugget", 0.55)));
        andesite.addProperty("processing_time", 400);
        emit.accept("crushing", "crushed_andesite_from_andesite", andesite);

        // Scrap is netherite's raw form, one per ancient debris as one raw iron per iron ore, so it takes
        // the raw shape and the debris takes the ore-block one.
        JsonObject scrap = recipe("create:crushing",
                List.of(CREATE, enabled("crushed_netherite_scrap")));
        scrap.add("ingredients", array(itemIngredient("minecraft:netherite_scrap")));
        scrap.add("results", array(
                itemResult("createmetalwork:crushed_netherite_scrap"),
                chanceResult("create:experience_nugget", 0.75)));
        scrap.addProperty("processing_time", CRUSH_TIME);
        emit.accept("crushing", "crushed_netherite_scrap_from_scrap", scrap);
        oreCrushing(emit, "crushed_netherite_scrap", "minecraft:ancient_debris", "minecraft:netherrack");

        ownCrushing(emit);
    }

    /**
     * Crushing for the crushed items we register ourselves, which nothing has produced since 3.0 --
     * every {@code crushed_raw_*} for the Mythic Metals, BetterEnd and BetterNether metals was
     * registered and unobtainable, and with them every {@code molten_<metal>_from_crushed_raw} route.
     * 1.20.1 had the lot.
     *
     * <p>400 ticks and a 0.75 experience nugget is 1.20.1's shape and Create's own figure for every
     * raw-ore crushing recipe. Tungsten is left out because Metallurgy and Northstar each crush
     * {@code c:raw_materials/tungsten} into their own crushed item, so a recipe of ours would put two
     * crushing recipes on one ore; we no longer ship a crushed tungsten. Deliberately not restored either:
     * 1.20.1's {@code _from_crushing_block} variant, which crushed the ingot block into nine crushed
     * items -- at {@link #CRUSHED_AMOUNT} that turns 810 mB into 1620, a duplicator in a crafting
     * table. 1.20.1 carried the same loop at its own 111 and 222.
     */
    private void ownCrushing(Emitter emit) {
        for (String metal : OWN_CRUSHED) {
            String rawTag = "c:raw_materials/" + metal;
            String id = crushedId(null, metal);

            JsonObject json = recipe("create:crushing",
                    List.of(CREATE, hasItems(rawTag), enabled(ownId(id))));
            json.add("ingredients", array(tagIngredient(rawTag)));
            json.add("results", array(
                    itemResult(id), chanceResult("create:experience_nugget", 0.75)));
            json.addProperty("processing_time", CRUSH_TIME);
            emit.accept("crushing", "crushed_raw_" + metal + "_from_raw", json);
        }

        // The three with no raw form at all: Mythic Metals drops a gem, so the ore block is the input
        // for the two that melt, and the gem itself for the one that does not.
        //
        // Starrite and morkite both melt, so crushing their GEM would be a duplicator -- the gem melts
        // at UNIT through c:ingots/<name> and its crushed form at twice that, casting back to two gems.
        // Their ore needs silk touch, which is the price of not shipping that. Unobtainium has no molten
        // form at all, so the gem is safe, and it is the one that matters: eight recipes need
        // c:crushed_unobtainium and nothing has ever produced it, leaving celestium and metallurgium
        // uncraftable.
        //
        // One recipe per ore block rather than one on mythicmetals:ores/<name>, because each gives back
        // its own host rock; the blocks are the members of those tags in Mythic Metals 0.24.6.
        oreCrushing(emit, "crushed_starrite", "mythicmetals:starrite_ore", "minecraft:cobblestone");
        oreCrushing(emit, "crushed_starrite", "mythicmetals:calcite_starrite_ore", "minecraft:calcite");
        oreCrushing(emit, "crushed_starrite", "mythicmetals:end_stone_starrite_ore", "minecraft:end_stone");
        oreCrushing(emit, "crushed_morkite", "mythicmetals:morkite_ore", "minecraft:cobblestone");
        oreCrushing(emit, "crushed_morkite", "mythicmetals:deepslate_morkite_ore", "minecraft:cobbled_deepslate");

        JsonObject gem = recipe("create:crushing",
                List.of(CREATE, LoadCondition.modLoaded(Metals.MYTHIC_METALS), enabled("crushed_unobtainium")));
        gem.add("ingredients", array(tagIngredient("c:ingots/unobtainium")));
        gem.add("results", array(itemResult(CreateMetalworkCommon.MOD_ID + ":crushed_unobtainium"),
                chanceResult("create:experience_nugget", 0.75)));
        gem.addProperty("processing_time", CRUSH_TIME);
        emit.accept("crushing", "crushed_unobtainium_from_gem", gem);

        // The shard is cincinnasite's raw form and goes through the loop above. This is the Silk Touch
        // path.
        oreCrushing(emit, "crushed_raw_cincinnasite", "betternether:cincinnasite_ore", "minecraft:netherrack");
    }

    /** Create's time for crushing a deepslate, Nether or gem ore block, against 400 for a raw ore. */
    private static final int ORE_CRUSH_TIME = 350;

    /**
     * An ore block shaped like Create's deepslate, Nether and gem ores: two crushed and a 25% chance of
     * a third, a 75% experience nugget and a 12.5% chance of the host rock back. The extra crushed is
     * the Silk Touch premium, standing in for the Fortune the block could not get.
     *
     * <p>Every block here is one of those three -- starrite and morkite drop gems, cincinnasite and
     * ancient debris sit in netherrack -- so none takes the stone metal-ore shape Create gives iron ore
     * (250 ticks, one and a 75% chance of a second), even starrite and morkite in plain stone.
     */
    private void oreCrushing(Emitter emit, String configId, String ore, String host) {
        String id = CreateMetalworkCommon.MOD_ID + ":" + configId;
        String mod = ore.split(":")[0];

        List<LoadCondition> conditions = new ArrayList<>();
        conditions.add(CREATE);
        if (!mod.equals("minecraft")) conditions.add(LoadCondition.modLoaded(mod));
        conditions.add(enabled(configId));

        JsonObject json = recipe("create:crushing", conditions);
        json.add("ingredients", array(itemIngredient(ore)));
        json.add("results", array(itemResult(id, 2), chanceResult(id, 0.25),
                chanceResult("create:experience_nugget", 0.75), chanceResult(host, 0.125)));
        json.addProperty("processing_time", ORE_CRUSH_TIME);
        emit.accept("crushing", configId + "_from_" + ore.split(":")[1], json);
    }

    // unit is how many of the crushed item make an ingot's worth: nine for andesite, whose crushed form
    // melts at a ninth, one everywhere else. Doubling multiplies that, not the item count.
    private record Doubling(String name, JsonObject input, String crushed, int unit,
                            List<JsonObject> bonus, int processingTime, List<LoadCondition> extra) {}

    private static final String SOD = "create_simple_ore_doubling";

    private static final int LAVA = 50;

    /**
     * Metals whose ore Simple Ore Doubling does not double itself, with the owner of the crushed item
     * it doubles into. Every owner spells it {@code <owner>:crushed_raw_<metal>}, so {@link #crushedId}
     * derives the id and only ownership has to be recorded.
     *
     * <p>SOD ships its own doubling for iron, copper, gold and zinc, so those are absent. Quicksilver is
     * absent because Create has the crushed item but {@code Metals.ALL} has no such metal. What is left
     * is the gap: eight metals Create ships a crushed item for, lithium and tungsten -- whose owners
     * ship a crushed item and no doubling, which this list once assumed they did -- and the eighteen
     * we ship one for ourselves.
     */
    private record Crushed(String metal, List<String> owners, List<LoadCondition> extra) {}

    /** Ours, which {@code enabled} then gates -- the {@code null} owner is the unconditional variant. */
    private static Crushed crushed(String metal) {
        List<String> owners = new ArrayList<>();
        owners.add(null);
        return new Crushed(metal, owners, List.of());
    }

    private static Crushed crushedFrom(String metal, String owner, LoadCondition... extra) {
        return new Crushed(metal, List.of(owner), List.of(extra));
    }

    private static final String MORE_ADDITIONS = "create_more_additions";

    /**
     * Metals whose {@code crushed_raw_*} is ours, so we are the only possible source. Tungsten is
     * absent: Metallurgy and Northstar each ship and crush their own. Titanium and iridium have raw
     * ores in Northstar and ATO but no crushed form anywhere, so without these the chain stopped at raw.
     */
    private static final List<String> OWN_CRUSHED = List.of(
            "adamantite", "aquarium", "banglum", "carmot", "cincinnasite", "iridium", "kyber",
            "manganese", "midas_gold", "mythril", "orichalcum", "palladium", "prometheum", "quadrillum",
            "runite", "stormyx", "thallasium", "titanium");

    private static final List<Crushed> CRUSHED = List.of(
            // Create's thirteen, less the four SOD covers and the quicksilver we have no metal for.
            crushedFrom("aluminum", "create"),
            crushedFrom("lead", "create"),
            crushedFrom("nickel", "create"),
            crushedFrom("osmium", "create"),
            crushedFrom("platinum", "create"),
            crushedFrom("uranium", "create"),
            // SOD doubles these two itself, but only behind a compat mod, and its recipe then produces
            // that mod's crushed item from that mod's raw one. Stand aside where it does, or the wheels
            // hold two recipes for one ore that differ in which crushed item they make.
            crushedFrom("silver", "create", LoadCondition.modAbsent(MORE_ADDITIONS)),
            crushedFrom("tin", "create", LoadCondition.modAbsent(Metals.IRONWORKS)),
            // Their owners crush these ores and ship no doubling. Metallurgy's tungsten wins where both
            // it and Northstar are installed, as it does for casting.
            crushedFrom("lithium", Metals.TFMG),
            new Crushed("tungsten", List.of(Metals.METALLURGY, Metals.NORTHSTAR), List.of()),
            // Ours. Mythic Metals ships no crushed item for any of its metals, and neither does
            // BetterNether or BetterEnd, so these are the only ones. Cincinnasite's raw form is
            // BetterNether's shard, bridged into c:raw_materials/cincinnasite by the tag provider.
            crushed("adamantite"), crushed("aquarium"), crushed("banglum"), crushed("carmot"),
            crushed("cincinnasite"), crushed("kyber"), crushed("manganese"), crushed("midas_gold"),
            crushed("mythril"), crushed("orichalcum"), crushed("palladium"), crushed("prometheum"),
            crushed("quadrillum"), crushed("runite"), crushed("stormyx"), crushed("thallasium"),
            // Ours as well, for Northstar's and ATO's raw ores.
            crushed("iridium"), crushed("titanium"));

    private static String crushedId(String owner, String metal) {
        String namespace = owner != null ? owner : CreateMetalworkCommon.MOD_ID;
        return namespace + ":crushed_raw_" + metal;
    }

    private void oreDoubling(Emitter emit) {
        List<Doubling> subjects = new ArrayList<>(List.of(
                new Doubling("crushed_andesite", itemIngredient("minecraft:andesite"),
                        "createmetalwork:crushed_andesite", 9, List.of(), 0, List.of()),
                new Doubling("crushed_netherite_scrap", itemIngredient("minecraft:netherite_scrap"),
                        "createmetalwork:crushed_netherite_scrap", 1,
                        List.of(chanceResult("createmetalwork:crushed_netherite_scrap", 0.5),
                                itemResult(SOD + ":slag")),
                        700, List.of())));

        for (Crushed subject : CRUSHED) {
            String rawTag = "c:raw_materials/" + subject.metal();
            for (Variant<String> owner : Variant.exclusive(subject.owners(), subject.owners())) {
                List<LoadCondition> extra = new ArrayList<>(subject.extra());
                extra.add(hasItems(rawTag));
                extra.addAll(owner.conditions());

                subjects.add(new Doubling("crushed_raw_" + subject.metal() + owner.suffix(),
                        tagIngredient(rawTag), crushedId(owner.value(), subject.metal()),
                        1, List.of(), 0, extra));
            }
        }

        for (Doubling subject : subjects) {
            doubling(emit, subject, false);
            doubling(emit, subject, true);
        }
    }

    private void doubling(Emitter emit, Doubling subject, boolean heated) {
        List<LoadCondition> conditions = new ArrayList<>();
        conditions.add(CREATE);
        conditions.add(LoadCondition.modLoaded(SOD));
        String mine = ownId(subject.crushed());
        if (mine != null) conditions.add(enabled(mine));
        conditions.addAll(subject.extra());

        JsonObject json = recipe("create:compacting", conditions);

        List<JsonObject> ingredients = new ArrayList<>();
        ingredients.add(subject.input());
        if (!heated) ingredients.add(concreteFluidIngredient("minecraft:lava", LAVA));
        json.add("ingredients", array(ingredients));

        List<JsonObject> results = new ArrayList<>();
        results.add(itemResult(subject.crushed(), subject.unit() * (heated ? 3 : 2)));
        results.addAll(subject.bonus());
        results.add(chanceResult(SOD + ":slag", heated ? 0.15 : 0.05));
        json.add("results", array(results));

        if (subject.processingTime() > 0) json.addProperty("processing_time", subject.processingTime());
        if (heated) json.addProperty("heat_requirement", "heated");

        emit.accept("compacting/ore_doubling",
                subject.name() + (heated ? "_heated" : "_with_lava"), json);
    }

    /**
     * The washed byproduct each dirty dust gives up, per metal. A trailing "*n" means that many, and a
     * leading "@" names a metal whose nugget it is -- see {@link #byproducts}.
     */
    private record Dust(String metal, String washed, List<String> owners) {}

    private static Dust dust(String metal, String washed, String... owners) {
        List<String> all = new ArrayList<>(Arrays.asList(owners));
        all.add(null);
        return new Dust(metal, washed, all);
    }

    private static final String CLAY = "minecraft:clay_ball";
    private static final String QUARTZ = "minecraft:quartz";
    private static final String REDSTONE = "minecraft:redstone";
    private static final String GUNPOWDER = "minecraft:gunpowder";
    private static final String FLINT = "minecraft:flint";

    private static final List<Dust> DUSTS = List.of(
            dust("adamantite", "create:powdered_obsidian"),
            dust("andesite", CLAY),
            // Quartz is a Nether byproduct, so it stays on the ores that spawn in netherrack: cincinnasite,
            // midas gold, palladium and stormyx. The rest take something from where they spawn, read from
            // the ore worldgen in Mythic Metals 0.24.6 and BetterEnd 21.0.11; FLINT is the plain stone
            // byproduct for the Overworld ores with nothing more particular to them.
            // Aquarium spawns only in aquatic biomes.
            dust("aquarium", "minecraft:prismarine_shard"),
            // The explosive metal.
            dust("banglum", GUNPOWDER),
            dust("carmot", FLINT),
            dust("cincinnasite", QUARTZ),
            // Kyber also spawns in calcite, the rock that lines amethyst geodes.
            dust("kyber", "minecraft:amethyst_shard"),
            dust("lead", "create:zinc_nugget*2", Metals.ATO, Metals.MEKANISM),
            dust("lithium", GUNPOWDER),
            // Most manganese is mined with iron ore, and goes into steel.
            dust("manganese", "minecraft:iron_nugget"),
            dust("midas_gold", QUARTZ),
            dust("mythril", FLINT),
            dust("netherite_scrap", "minecraft:gold_nugget*2"),
            // Nickel ore is mined with copper ore, and the two are refined apart.
            dust("nickel", "create:copper_nugget", Metals.ATO),
            // Orichalcum is the copper alloy of myth.
            dust("orichalcum", "create:copper_nugget"),
            dust("palladium", QUARTZ),
            dust("prometheum", FLINT),
            dust("quadrillum", FLINT),
            dust("runite", FLINT),
            // Starrite spawns in calcite and end stone as well as stone; amethyst, as kyber.
            dust("starrite", "minecraft:amethyst_shard"),
            dust("stormyx", QUARTZ),
            // Thallasium spawns in end stone. BetterEnd's own dust, which is safe to name: our thallasium
            // dusts register only with BetterEnd installed.
            dust("thallasium", "betterend:ender_dust"),
            dust("tin", REDSTONE, Metals.ATO, Metals.MEKANISM),
            // Ilmenite, the commonest titanium ore, is iron titanium oxide.
            dust("titanium", "minecraft:iron_nugget*2"),
            // ATO's own iridium processing gives up osmium as its byproduct. Only ATO has iridium, so its
            // dusts are always the ones used; ours is never registered.
            dust("iridium", "alltheores:osmium_nugget", Metals.ATO),
            // A gold nugget, as Metallurgy's own dirty tungsten washes; washTungsten brings Northstar's
            // crushed tungsten in line with it.
            dust("tungsten", "minecraft:gold_nugget", Metals.METALLURGY),
            // No unobtainium here: it is a crushed-only material with no molten form, so it has no
            // dirty dust for the chain to mill into.

            // These nine borrowed another mod's dust entirely until now, so the chain did not exist on
            // Create alone. Ours comes last, after every mod that ships one, so it registers only where
            // none of them is installed -- see their @absent clauses in ConfigDefaults.
            dust("copper", CLAY, Metals.METALLURGY, Metals.ATO, Metals.MEKANISM),
            dust("gold", QUARTZ, Metals.METALLURGY, Metals.ATO, Metals.MEKANISM),
            dust("iron", REDSTONE, Metals.METALLURGY, Metals.ATO, Metals.MEKANISM),
            dust("zinc", GUNPOWDER, Metals.METALLURGY, Metals.ATO),
            // Bauxite's red colour is iron oxide, left behind as red mud when the alumina is drawn off.
            dust("aluminum", "minecraft:iron_nugget", Metals.ATO),
            // Osmium and iridium occur as one natural alloy, osmiridium: the mirror of iridium's osmium.
            dust("osmium", "@iridium", Metals.ATO, Metals.MEKANISM),
            // Osmium is one of the platinum group, recovered from the same ore.
            dust("platinum", "@osmium", Metals.ATO),
            // Most silver is a byproduct of refining lead ore.
            dust("silver", "@lead", Metals.ATO),
            // Lead is where uranium's decay chain ends, so every uranium ore carries some.
            dust("uranium", "@lead", Metals.ATO, Metals.MEKANISM));

    private static final int GRIND_TIME = 150;

    /** Create's own time for every raw-ore crushing recipe, and its crushing times generally. */
    private static final int CRUSH_TIME = 400;

    private static final double GRIND_BONUS = 0.25;

    private void dustChain(Emitter emit) {
        for (Dust dust : DUSTS) {
            String crushedTag = "c:crushed_raw_materials/" + dust.metal();
            String dirtyTag = "c:dirty_dusts/" + dust.metal();

            for (Variant<String> owner : dustVariants(dust)) {
                String clean = dustId(owner.value(), dust.metal(), false);
                String dirty = dustId(owner.value(), dust.metal(), true);

                // Metallurgy ships this millstone-and-fan chain itself, in Create's namespace, for
                // exactly the five metals it owns a dust for. Four are identical to ours line for
                // line; its tungsten dust washes to a gold nugget where ours gives two zinc. So
                // emitting ours would put two millstone recipes on one crushed ore, and two
                // splashing recipes with different byproducts on one dirty dust.
                if (!Metals.METALLURGY.equals(owner.value())) {
                    List<LoadCondition> grind = new ArrayList<>();
                    grind.add(CREATE);
                    grind.add(hasItems(crushedTag));
                    grind.addAll(owner.conditions());
                    if (owner.value() == null) grind.add(enabled(ownId(dirty)));

                    JsonObject milling = recipe("create:milling", grind);
                    milling.add("ingredients", array(tagIngredient(crushedTag)));
                    milling.addProperty("processing_time", GRIND_TIME);
                    milling.add("results", array(itemResult(dirty), chanceResult(dirty, GRIND_BONUS)));
                    emit.accept("milling", "dirty_" + dust.metal() + "_dust" + owner.suffix(), milling);

                    // Fan washing has no processing time, and Create rejects a recipe that gives one.
                    List<LoadCondition> wash = new ArrayList<>();
                    wash.add(CREATE);
                    wash.add(hasItems(dirtyTag));
                    wash.addAll(owner.conditions());
                    if (owner.value() == null) wash.add(enabled(ownId(clean), ownId(dirty)));

                    for (Variant<String> by : byproducts(dust.washed())) {
                        List<LoadCondition> conditions = both(wash, by.conditions());
                        if (conditions == null) continue;
                        JsonObject splashing = recipe("create:splashing", conditions);
                        splashing.add("ingredients", array(tagIngredient(dirtyTag)));
                        splashing.add("results", array(itemResult(clean), washBonus(by.value())));
                        emit.accept("splashing", dust.metal() + "_dust_from_dirty"
                                + bySuffix(owner.suffix(), by), splashing);
                    }
                }

                // The wheels tier: a crushed ore straight to CLEAN dust, skipping the dirty stage and
                // the washing fan, two and a half for one. Nothing anywhere crushes a crushed item, so
                // this collides with nobody -- the millstone recipe above is untouched, and it is
                // Metallurgy's own for the five metals it ships.
                //
                // Wheels fall back to milling recipes, so both match a crushed ore in them; create:
                // crushing wins, exactly as Create's own gravel and wool rely on (a millstone mills
                // gravel to flint, the wheels crush it to sand). 400 ticks is Create's figure for every
                // raw-ore crushing recipe, and being slower per operation than the 150-tick millstone
                // is what leaves that tier a niche: faster per tick, and the only source of byproducts.
                List<LoadCondition> wheels = new ArrayList<>();
                wheels.add(CREATE);
                wheels.add(hasItems(crushedTag));
                wheels.addAll(owner.conditions());
                if (owner.value() == null) wheels.add(enabled(ownId(clean)));

                JsonObject crushing = recipe("create:crushing", wheels);
                crushing.add("ingredients", array(tagIngredient(crushedTag)));
                crushing.addProperty("processing_time", CRUSH_TIME);
                crushing.add("results", array(itemResult(clean, 2), chanceResult(clean, 0.5)));
                emit.accept("crushing", dust.metal() + "_dust_from_crushed" + owner.suffix(), crushing);
            }
        }
    }

    /**
     * Nuggets for a metal {@code Metals.ALL} records no Source for. Thallasium is unsourced so that
     * casting() leaves it to foreignCasts(); BetterEnd still ships a nugget to wash into.
     */
    private static final java.util.Map<String, Source> WASH_NUGGETS = java.util.Map.of(
            "thallasium", Source.from(Metals.BETTER_END, "betterend:thallasium_ingot",
                    "betterend:thallasium_nugget", null));

    /**
     * Fan washing for the crushed ores we ship ourselves: nine nuggets and the metal's washed byproduct,
     * as Create does for its own. Cincinnasite is absent because BetterNether ships no nugget.
     */
    private void washCrushed(Emitter emit) {
        for (String name : OWN_CRUSHED) {
            String tag = "c:crushed_raw_materials/" + name;
            String id = crushedId(null, name);
            Metal metal = byName(name);
            Source fallback = WASH_NUGGETS.get(name);
            List<Variant<Source>> sources = fallback != null
                    ? Variant.exclusive(List.of(fallback), java.util.Collections.singletonList(fallback.modId()))
                    : solidVariants(metal);

            for (Variant<Source> source : sources) {
                String nugget = source.value().nugget();
                if (nugget == null) continue;

                List<LoadCondition> conditions = new ArrayList<>();
                conditions.add(CREATE);
                conditions.add(hasItems(tag));
                conditions.addAll(source.conditions());
                conditions.add(enabled(ownId(id)));

                for (Variant<String> by : byproducts(dustOf(name).washed())) {
                    List<LoadCondition> all = both(conditions, by.conditions());
                    if (all == null) continue;
                    JsonObject json = recipe("create:splashing", all);
                    json.add("ingredients", array(tagIngredient(tag)));
                    json.add("results", array(itemResult(nugget, 9), washBonus(by.value())));
                    emit.accept("splashing", "crushed_raw_" + name + bySuffix(source.suffix(), by), json);
                }
            }
        }
    }

    /**
     * Create's compat crushed ores and the mods its own washing recipes wait on, with that mod's nugget,
     * transcribed from Create 6.0.10's data/create/recipe/splashing/<mod>/crushed_raw_<metal>.json.
     */
    private static final java.util.Map<String, java.util.Map<String, String>> CREATE_COMPAT_WASHING =
            java.util.Map.of(
                    "aluminum", java.util.Map.of(
                            "ic2", "ic2:nugget_aluminium",
                            "immersiveengineering", "immersiveengineering:nugget_aluminum"),
                    "lead", java.util.Map.of(
                            "immersiveengineering", "immersiveengineering:nugget_lead",
                            "mekanism", "mekanism:nugget_lead",
                            "oreganized", "oreganized:lead_nugget",
                            "thermal", "thermal:lead_nugget"),
                    "nickel", java.util.Map.of(
                            "immersiveengineering", "immersiveengineering:nugget_nickel",
                            "thermal", "thermal:nickel_nugget"),
                    "osmium", java.util.Map.of("mekanism", "mekanism:nugget_osmium"),
                    "silver", java.util.Map.of(
                            "galosphere", "galosphere:silver_nugget",
                            "ic2", "ic2:nugget_silver",
                            "iceandfire", "iceandfire:silver_nugget",
                            "immersiveengineering", "immersiveengineering:nugget_silver",
                            "oreganized", "oreganized:silver_nugget",
                            "thermal", "thermal:silver_nugget"),
                    "tin", java.util.Map.of(
                            "ic2", "ic2:nugget_tin",
                            "mekanism", "mekanism:nugget_tin",
                            "thermal", "thermal:tin_nugget"),
                    "uranium", java.util.Map.of(
                            "ic2", "ic2:nugget_uranium",
                            "immersiveengineering", "immersiveengineering:nugget_uranium",
                            "mekanism", "mekanism:nugget_uranium"),
                    // Neither Create nor any mod washes these two, though both crushed ores exist.
                    "platinum", java.util.Map.of(),
                    "lithium", java.util.Map.of());

    /** Every crushed ore above is Create's own, except TFMG's lithium. */
    private static String compatCrushed(String metal) {
        return metal.equals("lithium") ? "tfmg:crushed_raw_lithium" : "create:crushed_raw_" + metal;
    }

    /**
     * Fan washing for Create's compat crushed ores and TFMG's lithium, to match Create's four own and
     * our dirty dusts.
     *
     * <p>Create washes these only when one of a fixed list of mods supplies the nugget, and then into
     * nine nuggets with no byproduct, so with AllTheOres alone they could not be washed at all. Its
     * recipes are restated in place with the dirty dust's byproduct added; where none of those mods is
     * installed, ours washes into whichever mod's nugget is.
     */
    private void washCreateCompat(Emitter emit, ForeignEmitter foreign) {
        for (var entry : CREATE_COMPAT_WASHING.entrySet()) {
            String metal = entry.getKey();
            String crushed = compatCrushed(metal);
            List<Variant<String>> bys = byproducts(dustOf(metal).washed());
            List<LoadCondition> noneOfThem = new ArrayList<>();
            noneOfThem.add(CREATE);

            for (var mod : entry.getValue().entrySet()) {
                // Create's path holds one file, so it carries the first byproduct that can load beside
                // this mod and the rest sit in our namespace. Their conditions are exclusive, so with
                // the mod loaded exactly one of them does.
                boolean first = true;
                for (Variant<String> by : bys) {
                    List<LoadCondition> conditions = both(List.of(LoadCondition.modLoaded(mod.getKey())), by.conditions());
                    if (conditions == null) continue;
                    JsonObject json = recipe("create:splashing", conditions);
                    json.add("ingredients", array(itemIngredient(crushed)));
                    json.add("results", array(itemResult(mod.getValue(), 9), washBonus(by.value())));
                    if (first) foreign.accept("create", "splashing/" + mod.getKey() + "/crushed_raw_" + metal, json);
                    else emit.accept("splashing", "crushed_raw_" + metal + bySuffix("_from_" + mod.getKey(), by), json);
                    first = false;
                }
                noneOfThem.add(LoadCondition.modAbsent(mod.getKey()));
            }

            for (Variant<Source> source : solidVariants(byName(metal))) {
                String nugget = source.value().nugget();
                // A source from one of Create's listed mods could never load beside "none of them".
                if (nugget == null || entry.getValue().containsKey(nugget.split(":")[0])) continue;
                List<LoadCondition> conditions = new ArrayList<>(noneOfThem);
                conditions.addAll(source.conditions());

                for (Variant<String> by : bys) {
                    List<LoadCondition> all = both(conditions, by.conditions());
                    if (all == null) continue;
                    JsonObject json = recipe("create:splashing", all);
                    json.add("ingredients", array(itemIngredient(crushed)));
                    json.add("results", array(itemResult(nugget, 9), washBonus(by.value())));
                    emit.accept("splashing", "crushed_raw_" + metal + bySuffix(source.suffix(), by), json);
                }
            }
        }
    }

    /**
     * Crushed tungsten washing, on Metallurgy's byproduct. Northstar washed its crushed tungsten into
     * quartz and Metallurgy's dirty tungsten into a gold nugget; Northstar's is restated in place to the
     * gold nugget, and Metallurgy's crushed tungsten, which nothing washed, gets a recipe of its own.
     */
    private void washTungsten(Emitter emit, ForeignEmitter foreign) {
        JsonObject bonus = washBonus(dustOf("tungsten").washed());

        JsonObject northstar = recipe("create:splashing", List.of(LoadCondition.modLoaded(Metals.NORTHSTAR)));
        northstar.add("ingredients", array(itemIngredient("northstar:crushed_raw_tungsten")));
        northstar.add("results", array(itemResult("northstar:tungsten_nugget", 9), bonus));
        foreign.accept("create", "splashing/crushed_raw_tungsten", northstar);

        JsonObject metallurgy = recipe("create:splashing", List.of(CREATE, LoadCondition.modLoaded(Metals.METALLURGY)));
        metallurgy.add("ingredients", array(itemIngredient("createmetallurgy:crushed_raw_tungsten")));
        metallurgy.add("results", array(itemResult("createmetallurgy:tungsten_nugget", 9), bonus));
        emit.accept("splashing", "crushed_raw_tungsten_from_" + Metals.METALLURGY, metallurgy);
    }

    private static Dust dustOf(String metal) {
        for (Dust dust : DUSTS) {
            if (dust.metal().equals(metal)) return dust;
        }
        throw new IllegalArgumentException("No dust named " + metal + " in DUSTS");
    }

    private static final String ORE_HAMMERS = "alltheores:ore_hammers";

    /**
     * The round trip our own dusts never had. AllTheOres' and Mekanism's dusts can be made from an
     * ingot and smelted back; ours could only leave the ore chain and only through a heated mixer.
     *
     * <p>Break-even in both directions at {@link Amounts#UNIT}, which is the whole reason it is safe.
     * The ore hammer and crusher halves need no metal list: {@code modLoaded} on the tool's mod and
     * {@code enabled} on our dust cannot both hold for a metal whose dust defers to that same mod, so
     * those files drop out on their own. What is left live is mostly the Mythic Metals set, which has
     * no dust-producing recipe of its own at all.
     */
    private void dustRoundTrip(Emitter emit) {
        for (Dust dust : DUSTS) {
            String dustTag = "c:dusts/" + dust.metal();
            Metal metal = find(dust.metal());
            if (metal == null) continue;

            for (Variant<String> owner : dustVariants(dust)) {
                if (owner.value() != null) continue;
                String clean = dustId(null, dust.metal(), false);
                String ingotTag = "c:ingots/" + dust.metal();

                List<LoadCondition> hammer = new ArrayList<>();
                hammer.add(LoadCondition.modLoaded(Metals.ATO));
                hammer.add(hasItems(ingotTag));
                hammer.addAll(owner.conditions());
                hammer.add(enabled(ownId(clean)));

                // ATO's own shape. Its hammers are damageable, so a shapeless recipe hands the hammer
                // back damaged rather than consuming it.
                JsonObject smash = recipe("minecraft:crafting_shapeless", hammer);
                smash.addProperty("category", "misc");
                smash.add("ingredients", array(tagIngredient(ORE_HAMMERS), tagIngredient(ingotTag)));
                smash.add("result", itemResult(clean));
                emit.accept("crafting", dust.metal() + "_dust_from_ingot", smash);

                List<LoadCondition> crush = new ArrayList<>();
                crush.add(LoadCondition.modLoaded(Metals.MEKANISM));
                crush.add(hasItems(ingotTag));
                crush.addAll(owner.conditions());
                crush.add(enabled(ownId(clean)));

                JsonObject mek = recipe("mekanism:crushing", crush);
                mek.add("input", counted(tagIngredient(ingotTag)));
                mek.add("output", counted(itemResult(clean)));
                emit.accept("mekanism/crushing", dust.metal() + "_dust_from_ingot", mek);
            }

            // The way back, which needs no tool mod and so loads for every dust of ours.
            for (Variant<Source> source : solidVariants(metal)) {
                String ingot = source.value().ingot();
                if (ingot == null) continue;

                for (String type : List.of("minecraft:smelting", "minecraft:blasting")) {
                    List<LoadCondition> conditions = new ArrayList<>();
                    conditions.add(hasItems(dustTag));
                    conditions.addAll(source.conditions());

                    JsonObject json = recipe(type, conditions);
                    json.addProperty("category", "misc");
                    json.add("ingredient", tagIngredient(dustTag));
                    json.add("result", itemResult(ingot));
                    json.addProperty("experience", 0.1);
                    json.addProperty("cookingtime", type.endsWith("blasting") ? 100 : 200);
                    emit.accept(type.endsWith("blasting") ? "blasting" : "smelting",
                            dust.metal() + "_ingot_from_dust" + source.suffix(), json);
                }
            }
        }
    }

    /**
     * Compacts molten slag straight into Simple Ore Doubling's, for packs with no casting of their own.
     *
     * <p>At the one slag rate, {@code UNIT} per slag, like every other slag recipe here; see
     * {@link #slagSolids}. Two copies because the conditions can only AND: one for packs with TFMG,
     * one for Metallurgy without it.
     */
    private void slagSink(Emitter emit) {
        List<List<LoadCondition>> gates = List.of(
                List.of(CREATE, LoadCondition.modLoaded(SOD), LoadCondition.modLoaded(TFMG)),
                List.of(CREATE, LoadCondition.modLoaded(SOD), LoadCondition.modAbsent(TFMG),
                        LoadCondition.modLoaded(Metals.METALLURGY)));
        List<String> suffixes = List.of("", "_from_" + Metals.METALLURGY);

        for (int i = 0; i < gates.size(); i++) {
            JsonObject json = recipe("create:compacting", gates.get(i));
            json.add("ingredients", array(fluidIngredient(SLAG_TAG, UNIT)));
            json.add("results", array(itemResult(SOD + ":slag")));
            emit.accept("compacting", "slag_from_molten_slag" + suffixes.get(i), json);
        }
    }

    /**
     * TFMG's and Metallurgy's slag casts, restated in their own namespaces to read {@link #SLAG_TAG}
     * at the one slag rate: {@code UNIT} per slag and {@code BLOCK} per block. TFMG's own block was
     * 20 mB; Metallurgy's figures were already these. Times are the originals.
     */
    private void slagCasting(ForeignEmitter foreign) {
        JsonObject tfmg = recipe("tfmg:casting", List.of(LoadCondition.modLoaded(TFMG)));
        tfmg.add("ingredients", array(fluidIngredient(SLAG_TAG, BLOCK)));
        tfmg.addProperty("processing_time", 50);
        tfmg.add("results", array(itemResult("tfmg:slag_block")));
        foreign.accept(TFMG, "casting/slag_block", tfmg);

        List<LoadCondition> metallurgy = List.of(LoadCondition.modLoaded(Metals.METALLURGY));
        foreign.accept(Metals.METALLURGY, "casting_in_table/slag_casting",
                metallurgyCast("createmetallurgy:casting_in_table", metallurgy, UNIT, 60, "createmetallurgy:slag"));
        foreign.accept(Metals.METALLURGY, "casting_in_basin/slag_block_casting",
                metallurgyCast("createmetallurgy:casting_in_basin", metallurgy, BLOCK, 480, "createmetallurgy:slag_block"));
    }

    /**
     * Every recipe TFMG, Metallurgy and Simple Ore Doubling ship that consumes solid slag, restated in
     * place to read {@code c:slag} and {@code c:storage_blocks/slag}, so any mod's slag item or block
     * works in any mod's recipes.
     *
     * <p>That only holds if the three agree on what slag is worth, or the cheapest source sets the price
     * for everyone: TFMG cast a block from 20 mB and crushed it into 2.3 slag, so its slag fed into SOD's
     * washing would have paid about 35 nuggets per 1000 mB. So there is one rate, Metallurgy's: a slag is
     * {@code UNIT}, a block is nine slag and {@code BLOCK}. TFMG's casting and crushing move to it, and
     * its two mixtures are scaled so a millibucket of slag still makes what it made before -- see below.
     *
     * <p>Two mods packing, unpacking or crushing the same tag would leave the grid or the wheels a coin
     * toss, so one mod owns each where they overlap. Metallurgy packs and unpacks, SOD only without it;
     * SOD crushes blocks into its nuggets and experience, TFMG into slag only without SOD.
     */
    private void slagSolids(ForeignEmitter foreign) {
        LoadCondition hasTfmg = LoadCondition.modLoaded(TFMG);
        LoadCondition hasMetallurgy = LoadCondition.modLoaded(Metals.METALLURGY);
        LoadCondition hasSod = LoadCondition.modLoaded(SOD);
        List<LoadCondition> tfmg = List.of(hasTfmg);
        List<LoadCondition> metallurgy = List.of(hasMetallurgy);
        List<LoadCondition> sod = List.of(hasSod);
        List<LoadCondition> sodAlone = List.of(hasSod, LoadCondition.not(hasMetallurgy));

        // TFMG's slag mixtures made 32 from a slag worth 20 / 2.3 mB. At UNIT a slag that would cost
        // ten times the slag per mixture, so one slag now carries eight batches of bitumen or cement and
        // gravel: 256 for 90 mB, within a third of TFMG's figure, and 32 per bitumen exactly as before.
        // Eight because a basin recipe takes at most four result stacks.
        for (String[] mix : new String[][] {{"asphalt", "tfmg:bitumen"}, {"concrete", "tfmg:cement"}}) {
            List<JsonObject> ingredients = new ArrayList<>();
            ingredients.add(tagIngredient(SLAG_ITEM));
            for (int i = 0; i < SLAG_BATCHES; i++) {
                ingredients.add(itemIngredient(mix[1]));
                ingredients.add(itemIngredient("minecraft:gravel"));
            }
            JsonObject json = recipe("create:mixing", tfmg);
            json.add("ingredients", array(ingredients));
            json.add("results", array(stacks("tfmg:" + mix[0] + "_mixture", 32 * SLAG_BATCHES)));
            foreign.accept(TFMG, "mixing/" + mix[0] + "_mixture_from_slag", json);
        }

        // TFMG ships this one under Create's namespace. Nine, not 2.3: a block is nine slag.
        JsonObject tfmgCrush = recipe("create:crushing", List.of(hasTfmg, LoadCondition.not(hasSod)));
        tfmgCrush.add("ingredients", array(tagIngredient(SLAG_BLOCK)));
        tfmgCrush.add("results", array(itemResult("tfmg:slag", 9)));
        foreign.accept("create", "crushing/slag_block", tfmgCrush);

        // TFMG cut 4 bricks from a 20 mB block, 5 mB a brick. At 810 mB a block would need 162, past a
        // stonecutter's one stack, so the block's recipe is switched off and a single slag gives the
        // exact 18 instead.
        foreign.accept(TFMG, "slag_bricks_from_slag_block_stonecutting",
                switchedOff(TFMG, "minecraft:stonecutting"));

        JsonObject slagBricks = recipe("minecraft:stonecutting", tfmg);
        slagBricks.add("ingredient", tagIngredient(SLAG_ITEM));
        slagBricks.add("result", itemResult("tfmg:slag_bricks", UNIT / 5));
        foreign.accept(TFMG, "slag_bricks_from_slag_stonecutting", slagBricks);

        JsonObject tuff = recipe("create:compacting", metallurgy);
        JsonObject slag = tagIngredient(SLAG_ITEM);
        tuff.add("ingredients", array(slag, slag, slag, slag,
                itemIngredient("minecraft:gravel"), itemIngredient("minecraft:cobblestone")));
        tuff.add("results", array(itemResult("minecraft:tuff")));
        foreign.accept(Metals.METALLURGY, "compacting/tuff_from_slag", tuff);

        JsonObject pack = recipe("minecraft:crafting_shaped", metallurgy);
        pack.addProperty("category", "misc");
        JsonObject key = new JsonObject();
        key.add("S", tagIngredient(SLAG_ITEM));
        pack.add("key", key);
        JsonArray pattern = new JsonArray();
        for (int row = 0; row < 3; row++) pattern.add("SSS");
        pack.add("pattern", pattern);
        pack.add("result", itemResult("createmetallurgy:slag_block", 1));
        foreign.accept(Metals.METALLURGY, "crafting/materials/slag_block", pack);
        foreign.accept(Metals.METALLURGY, "crafting/materials/slag_from_block",
                unpack(metallurgy, "createmetallurgy:slag"));

        List<JsonObject> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) nine.add(tagIngredient(SLAG_ITEM));
        JsonObject sodPack = recipe("minecraft:crafting_shapeless", sodAlone);
        sodPack.addProperty("category", "misc");
        sodPack.add("ingredients", array(nine));
        sodPack.add("result", itemResult(SOD + ":slag_block", 1));
        foreign.accept(SOD, "slag_block_crafting", sodPack);
        foreign.accept(SOD, "slag_block_unpacking", unpack(sodAlone, SOD + ":slag"));

        // SOD's own path, typo and all.
        JsonObject washing = recipe("create:splashing", sod);
        washing.add("ingredients", array(tagIngredient(SLAG_ITEM)));
        washing.add("results", array(
                chanceResult("create:copper_nugget", 0.075),
                chanceResult("minecraft:gold_nugget", 0.075),
                chanceResult("minecraft:iron_nugget", 0.075),
                chanceResult("create:zinc_nugget", 0.075),
                chanceResult("create:experience_nugget", 0.35)));
        foreign.accept(SOD, "spashing/washing_slag", washing);

        JsonObject sodCrush = recipe("create:crushing", sod);
        sodCrush.add("ingredients", array(tagIngredient(SLAG_BLOCK)));
        sodCrush.addProperty("processing_time", 250);
        sodCrush.add("results", array(
                itemResult("create:experience_nugget", 3),
                chanceResult("create:experience_nugget", 0.5),
                chanceResult("create:copper_nugget", 0.75),
                chanceResult("minecraft:gold_nugget", 0.75),
                chanceResult("minecraft:iron_nugget", 0.75),
                chanceResult("create:zinc_nugget", 0.75)));
        foreign.accept(SOD, "crushing/crushing_slag_block", sodCrush);
    }

    /** Bitumen-or-cement-and-gravel batches one slag carries in TFMG's mixtures; see slagSolids. */
    private static final int SLAG_BATCHES = 8;

    private static JsonObject unpack(List<LoadCondition> conditions, String slag) {
        JsonObject json = recipe("minecraft:crafting_shapeless", conditions);
        json.addProperty("category", "misc");
        json.add("ingredients", array(tagIngredient(SLAG_BLOCK)));
        json.add("result", itemResult(slag, 9));
        return json;
    }

    /** A count over 64 split into stacks, since a result entry is one stack. */
    private static List<JsonObject> stacks(String item, int count) {
        List<JsonObject> results = new ArrayList<>();
        for (; count > 0; count -= 64) results.add(itemResult(item, Math.min(count, 64)));
        return results;
    }

    /** Metallurgy's result is a single {"item": stack}, not Create's results array. */
    private static JsonObject metallurgyCast(String type, List<LoadCondition> conditions, int amount,
                                             int time, String item) {
        JsonObject json = recipe(type, conditions);
        json.add("ingredients", array(fluidIngredient(SLAG_TAG, amount)));
        json.addProperty("processing_time", time);
        JsonObject stack = new JsonObject();
        stack.addProperty("count", 1);
        stack.addProperty("id", item);
        JsonObject result = new JsonObject();
        result.add("item", stack);
        json.add("result", result);
        return json;
    }

    /** Mekanism's crushing codec wants a count beside the tag or id, unlike Create's. */
    private static JsonObject counted(JsonObject base) {
        base.addProperty("count", 1);
        return base;
    }

    /** Null rather than throwing, since {@code DUSTS} covers materials {@code Metals.ALL} does not. */
    private static Metal find(String name) {
        for (Metal metal : Metals.ALL) {
            if (metal.name().equals(name)) return metal;
        }
        return null;
    }

    /**
     * Each way a byproduct can resolve. A fixed item is one unconditioned variant. "@metal" is a nugget
     * of that metal from whichever mod supplies one, in {@code Metals.ALL} order, and clay where none
     * does; suffixes read "_with_<mod>" so they do not echo the owner's "_from_<mod>".
     */
    private static List<Variant<String>> byproducts(String washed) {
        if (!washed.startsWith("@")) return List.of(new Variant<>(washed, List.of(), ""));
        int star = washed.indexOf('*');
        String count = star < 0 ? "" : washed.substring(star);
        Metal metal = byName(washed.substring(1, star < 0 ? washed.length() : star));

        List<String> items = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        for (Source source : metal.reachableSources()) {
            if (source.nugget() == null) continue;
            items.add(source.nugget() + count);
            owners.add(source.modId());
        }
        if (!owners.contains(null)) {
            items.add(CLAY);
            owners.add(null);
        }
        List<Variant<String>> variants = new ArrayList<>();
        for (Variant<String> v : Variant.exclusive(items, owners)) {
            variants.add(new Variant<>(v.value(), v.conditions(), v.suffix().replace("_from_", "_with_")));
        }
        return variants;
    }

    /**
     * Both lists ANDed, or null when they can never hold together because one needs a mod the other
     * needs absent. Crossing a dust's owners with its byproduct's suppliers would otherwise write files
     * that never load.
     */
    private static List<LoadCondition> both(List<LoadCondition> a, List<LoadCondition> b) {
        List<LoadCondition> all = new ArrayList<>(a);
        for (LoadCondition c : b) {
            if (all.contains(LoadCondition.not(c))
                    || (c instanceof LoadCondition.Not not && all.contains(not.inner()))) return null;
            if (!all.contains(c)) all.add(c);
        }
        return all;
    }

    /**
     * The owner's suffix, plus the byproduct's unless both name the same mod. Unique anyway: with the
     * owner's mod loaded, every other byproduct variant needs that mod absent and is dropped by
     * {@link #both}.
     */
    private static String bySuffix(String ownerSuffix, Variant<String> by) {
        return by.suffix().equals(ownerSuffix.replace("_from_", "_with_")) ? ownerSuffix : ownerSuffix + by.suffix();
    }

    private static JsonObject washBonus(String washed) {
        int star = washed.indexOf('*');
        return star < 0
                ? chanceResult(washed, WASH_BONUS)
                : chanceResult(washed.substring(0, star), WASH_BONUS,
                        Integer.parseInt(washed.substring(star + 1)));
    }

    /**
     * Clean and dirty id patterns for an owner that does not name dusts {@code <metal>_dust}. Same idea
     * as a {@code fluidOwners} entry containing a colon, which names the fluid outright for a mod that
     * breaks the convention.
     *
     * <p>Mekanism is the only one, and the only reason it can own this chain at all: it has both halves
     * and tags them as {@code c:dusts/<metal>} and {@code c:dirty_dusts/<metal>}, but spells them
     * {@code dust_iron} and {@code dirty_dust_iron}, so deriving a name would have produced an item
     * that does not exist. Only the seven metals it has a dirty dust for list it as an owner.
     */
    private record DustNaming(String clean, String dirty) {}

    private static final java.util.Map<String, DustNaming> DUST_NAMING = java.util.Map.of(
            Metals.MEKANISM, new DustNaming("mekanism:dust_%s", "mekanism:dirty_dust_%s"));

    private static String dustId(String owner, String metal, boolean dirty) {
        DustNaming naming = owner == null ? null : DUST_NAMING.get(owner);
        if (naming != null) return String.format(dirty ? naming.dirty() : naming.clean(), metal);

        String namespace = owner != null ? owner : CreateMetalworkCommon.MOD_ID;
        return namespace + ":" + (dirty ? "dirty_" : "") + metal + "_dust";
    }

    private static List<Variant<String>> dustVariants(Dust dust) {
        return Variant.exclusive(dust.owners(), dust.owners());
    }

    /**
     * @param variant appended to the end of the file name, after the owner suffix, to tell a second
     *                recipe for a metal from the first -- {@code ""} for the only one. Every emitter
     *                keys the name on the metal, so two entries sharing one would otherwise overwrite
     *                each other.
     */
    private record Alloy(String metal, String variant, int amount, Metal.Heat heat, String requires,
                         List<String> inputs, List<String> extras) {}

    private static Alloy alloy(String metal, int amount, Metal.Heat heat, String requires,
                              String inputs, String... extras) {
        return alloy(metal, "", amount, heat, requires, inputs, extras);
    }

    private static Alloy alloy(String metal, String variant, int amount, Metal.Heat heat,
                              String requires, String inputs, String... extras) {
        return new Alloy(metal, variant, amount, heat, requires,
                Arrays.asList(inputs.split(" ")), Arrays.asList(extras));
    }

    /**
     * Every output is twice what the owning mod's own recipe makes. Each entry's comment records that
     * mod's ratio, because the number here no longer shows it and getting the baseline wrong compounds:
     * durasteel and terminite were already at twice their source ratio with nothing saying so, and
     * doubling those as they stood would have shipped four times.
     */
    private static final List<Alloy> ALLOYS = List.of(
            // Ironworks and Metallurgy: three copper and a tin make four bronze.
            alloy("bronze", 720, Metal.Heat.HEATED, null, "270:copper 90:tin"),
            // BetterEnd alloys a thallasium and a netherite into two aeternium.
            alloy("aeternium", 360, Metal.Heat.HEATED, "betterend", "90:thallasium 90:netherite"),
            // Mythic Metals makes one celestium from a star platinum, a kyber, a carmot and an
            // unobtainium; kyrmot stands in for the kyber and carmot it is mixed from.
            alloy("celestium", 180, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:star_platinum 90:kyrmot",
                    "#c:crushed_unobtainium"),
            // Mythic Metals' alloy_durasteel_from_ingots makes TWO durasteel from a quadrillum and a
            // manganese, so the faithful figure is 180 and this is it doubled. It read 360 before, which
            // was already their ratio twice over.
            alloy("durasteel", 360, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:quadrillum 90:manganese"),
            // Mythic Metals makes one hallowed from an adamantite, a mythril and an orichalcum;
            // mythantite stands in for the first two.
            alloy("hallowed", 180, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:mythantite 90:orichalcum"),
            alloy("kyrmot", 180, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:kyber 90:carmot"),
            // Mythic Metals makes one metallurgium from a hallowed, a palladium and an unobtainium.
            alloy("metallurgium", 180, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
                    "90:mythantite 90:orichadium", "#c:crushed_unobtainium"),
            alloy("mythantite", 180, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:mythril 90:adamantite"),
            alloy("orichadium", 180, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:orichalcum 90:palladium"),
            // Mythic Metals makes one star platinum from a starrite and a platinum.
            alloy("star_platinum", 180, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:starrite 90:platinum"),
            // BetterEnd's terminite_ingot is bclib:alloying, one iron and one ender dust for ONE
            // terminite, so the faithful figure is 90 and this is it doubled. It read 180 before, which
            // was already their ratio twice over.
            alloy("terminite", 180, Metal.Heat.HEATED, "betterend", "90:iron", "betterend:ender_dust"),
            // Northstar makes one steel from two iron and volcanic ash.
            alloy("steel", UNIT * 2, Metal.Heat.HEATED, Metals.NORTHSTAR, "180:iron",
                    "northstar:volcanic_ash"),
            // TFMG mixes two nickel, one silicon and two steel into two magnetic alloy ingots.
            alloy("magnetic_alloy", UNIT * 4, Metal.Heat.HEATED, TFMG,
                    "180:nickel 90:silicon 180:steel"),

            // Alloys the mods that own the metal only ever make out of solids.
            // All The Ores blends a gold and a silver dust into two electrum.
            alloy("electrum", UNIT * 4, Metal.Heat.HEATED, null, "90:gold 90:silver"),
            // All The Ores: three lead, one platinum and two ender pearls make four enderium.
            alloy("enderium", UNIT * 8, Metal.Heat.HEATED, null, "270:lead 90:platinum",
                    "minecraft:ender_pearl", "minecraft:ender_pearl"),
            // All The Ores blends a copper and a nickel dust into two constantan; Metallurgy alloys them
            // one to one as well.
            alloy("constantan", UNIT * 4, Metal.Heat.HEATED, null, "90:copper 90:nickel"),
            // All The Ores: two iron and a nickel make three invar. Metallurgy's 20:10 -> 30 agrees.
            alloy("invar", UNIT * 6, Metal.Heat.HEATED, null, "180:iron 90:nickel"),
            // All The Ores: three tin, one silver and four glowstone dust make four lumium. Both of these
            // are stated at half a batch, same ratio: Metallurgy's alloyer rejects more than three item
            // inputs, and four dust would fail to parse there.
            alloy("lumium", UNIT * 4, Metal.Heat.HEATED, null, "135:tin 45:silver",
                    "minecraft:glowstone_dust", "minecraft:glowstone_dust"),
            // All The Ores: three copper, one silver and four redstone make four signalum.
            alloy("signalum", UNIT * 4, Metal.Heat.HEATED, null, "135:copper 45:silver",
                    "minecraft:redstone", "minecraft:redstone"),
            // Metallurgy alloys two netherite scrap and 120 silver into 45 necromium.
            alloy("necromium", 90, Metal.Heat.SUPERHEATED, null, "120:silver",
                    "minecraft:netherite_scrap", "minecraft:netherite_scrap"),
            // Metallurgy alloys an andesite alloy and 60 tungsten into 150 obdurium.
            alloy("obdurium", 300, Metal.Heat.SUPERHEATED, null, "60:tungsten", "create:andesite_alloy"),
            // Metallurgy alloys an ender pearl and 90 netherite into 90 void steel.
            alloy("void_steel", 180, Metal.Heat.SUPERHEATED, null, "90:netherite", "minecraft:ender_pearl"),
            // Big Cannons superheats a netherite scrap with four steel into eight nethersteel, or with
            // eight cast iron for the same eight. Both, since cast iron is the way in for anyone who has
            // not got to steel yet, and both double here so the two routes stay level.
            alloy("nethersteel", UNIT * 16, Metal.Heat.SUPERHEATED, null, "360:steel",
                    "minecraft:netherite_scrap"),
            alloy("nethersteel", "_alt", UNIT * 16, Metal.Heat.SUPERHEATED, null, "720:cast_iron",
                    "minecraft:netherite_scrap"));

    private static List<JsonObject> alloyIngredients(Alloy a) {
        List<JsonObject> ingredients = new ArrayList<>();
        for (String in : a.inputs()) {
            String[] parts = in.split(":");
            ingredients.add(fluidIngredient("c:molten_" + parts[1], Integer.parseInt(parts[0])));
        }
        for (String extra : a.extras()) {
            ingredients.add(extra.startsWith("#")
                    ? tagIngredient(extra.substring(1)) : itemIngredient(extra));
        }
        return ingredients;
    }

    private static final List<String> METALLURGY_ALLOYS = List.of(
            "brass", "bronze", "constantan", "electrum", "invar", "necromium", "netherite", "obdurium",
            "steel", "void_steel");

    private void fantasyAlloys(Emitter emit) {
        for (Alloy a : ALLOYS) {
            List<JsonObject> ingredients = alloyIngredients(a);

            List<LoadCondition> shared = new ArrayList<>();
            if (a.requires() != null) shared.add(LoadCondition.modLoaded(a.requires()));

            for (Variant<String> fluid : fluidVariants(byName(a.metal()))) {
                List<LoadCondition> basin = new ArrayList<>(shared);
                basin.add(0, CREATE);
                basin.addAll(fluid.conditions());
                JsonObject mixing = recipe("create:mixing", basin);
                mixing.add("ingredients", array(ingredients));
                mixing.add("results", array(fluidResult(fluid.value(), a.amount())));
                mixing.addProperty("heat_requirement", a.heat().json);
                emit.accept("mixing/alloying", "molten_" + a.metal() + fluid.suffix() + a.variant(), mixing);

                if (a.requires() == null && METALLURGY_ALLOYS.contains(a.metal())) continue;
                List<LoadCondition> metallurgy = new ArrayList<>(shared);
                metallurgy.add(0, LoadCondition.modLoaded(Metals.METALLURGY));
                metallurgy.addAll(fluid.conditions());
                JsonObject alloying = recipe("createmetallurgy:alloying", metallurgy);
                alloying.addProperty("heat_requirement", a.heat().json);
                alloying.add("ingredients", array(ingredients));
                alloying.addProperty("processing_time", 100);
                alloying.add("results", array(fluidResult(fluid.value(), a.amount())));
                emit.accept("metallurgy/alloying", "molten_" + a.metal() + fluid.suffix() + a.variant(), alloying);
            }
        }
    }

    /** Doubled alongside {@link #ALLOYS}, so skipping the intermediates is neither a bonus nor a tax. */
    private static final List<Alloy> COLLAPSED = List.of(
            alloy("hallowed", UNIT * 2, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
                    "90:mythril 90:adamantite 90:orichalcum"),
            alloy("metallurgium", UNIT * 2, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
                    "90:mythril 90:adamantite 90:orichalcum 90:palladium", "#c:crushed_unobtainium"),
            alloy("celestium", UNIT * 2, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
                    "90:starrite 90:platinum 90:kyber 90:carmot", "#c:crushed_unobtainium"));

    private void collapsedAlloys(Emitter emit) {
        for (Alloy a : COLLAPSED) {
            List<JsonObject> ingredients = alloyIngredients(a);
            for (Variant<String> fluid : fluidVariants(byName(a.metal()))) {
                List<LoadCondition> conditions = new ArrayList<>();
                conditions.add(CREATE);
                if (a.requires() != null) conditions.add(LoadCondition.modLoaded(a.requires()));
                conditions.addAll(fluid.conditions());

                JsonObject json = recipe("create:mixing", conditions);
                json.add("ingredients", array(ingredients));
                json.add("results", array(fluidResult(fluid.value(), a.amount())));
                json.addProperty("heat_requirement", a.heat().json);
                emit.accept("mixing/alloying", "molten_" + a.metal() + "_direct" + fluid.suffix(), json);
            }
        }
    }

    private static final String INGOT_MOLD = "createmetallurgy:graphite_ingot_mold";

    private void metallurgyParity(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(Metals.METALLURGY);

        for (String name : parityMetals()) {
            Metal metal = byName(name);

                // Naming this mod's namespace outright breaks for any metal it defers to another mod.
            for (Variant<String> fluid : fluidVariants(metal)) {
                String id = fluid.value();
                String suffix = fluid.suffix();

                String heat = metal.heat().json;

                // Metallurgy's own figures, form for form, rather than one flat rate: it melts a
                // nugget in 4 ticks and an ingot in 40, and sets a heat requirement on every one of
                // its 231 melting recipes. Without these ours melted cold, and slower than the mod's
                // own metals in the mod's own machine.
                melt(emit, name, "nugget", "c:nuggets/" + name, id, NUGGET, 4, 0, heat, present, fluid, suffix);
                melt(emit, name, "ingot", "c:ingots/" + name, id, UNIT, 40, 0, heat, present, fluid, suffix);
                melt(emit, name, "dust", "c:dusts/" + name, id, UNIT, 20, 0, heat, present, fluid, suffix);
                melt(emit, name, "dirty_dust", "c:dirty_dusts/" + name, id, UNIT, 30, SLAG_DIRTY,
                        heat, present, fluid, suffix);
                melt(emit, name, "raw", "c:raw_materials/" + name, id, RAW_AMOUNT, 40, SLAG_RAW,
                        heat, present, fluid, suffix);

                // Doubled like every other crushed melt, which does mean our metals out-yield the
                // eleven Metallurgy ships a crushed form for, in its own melter. That is the cost
                // tfmgSteel() already pays to restate TFMG's 144 mB ingot at 90: one value for a
                // crushed ore everywhere, rather than a purpose-built melting machine paying 1x
                // where a plain heated mixer pays 2x. Metallurgy's 48 ticks are kept either way, so
                // the melter now melts a crushed ore at twice the mixer's rate, 180 in 48 against 100.
                melt(emit, name, "raw_crushed", "c:crushed_raw_materials/" + name, id, CRUSHED_AMOUNT,
                        48, SLAG_RAW, heat, present, fluid, suffix);

                List<LoadCondition> bulkConditions = new ArrayList<>();
                bulkConditions.add(present);
                bulkConditions.add(hasItems("c:storage_blocks/" + name));
                bulkConditions.addAll(fluid.conditions());
                JsonObject bulk = recipe("createmetallurgy:bulk_melting", bulkConditions);
                bulk.add("ingredients", array(tagIngredient("c:storage_blocks/" + name)));
                bulk.addProperty("minHeatRequirement", 3);
                bulk.addProperty("processing_time", 224);
                bulk.add("results", array(fluidResult(id, BLOCK)));
                emit.accept("metallurgy/bulk_melting", name + suffix, bulk);

                // Metallurgy's recipes take a tag as the result, so this lands on whichever mod's ingot is present.
                List<LoadCondition> tableConditions = new ArrayList<>();
                tableConditions.add(present);
                tableConditions.add(hasItems("c:ingots/" + name));
                tableConditions.addAll(fluid.conditions());
                JsonObject cast = recipe("createmetallurgy:casting_in_table", tableConditions);
                cast.add("ingredients", array(
                        concreteFluidIngredient(id, UNIT), itemIngredient(INGOT_MOLD)));
                cast.addProperty("processing_time", 60);
                cast.add("result", tagResult("c:ingots/" + name));
                emit.accept("metallurgy/casting_in_table", name + suffix, cast);

                List<LoadCondition> basinConditions = new ArrayList<>();
                basinConditions.add(present);
                basinConditions.add(hasItems("c:storage_blocks/" + name));
                basinConditions.addAll(fluid.conditions());
                JsonObject basin = recipe("createmetallurgy:casting_in_basin", basinConditions);
                basin.add("ingredients", array(concreteFluidIngredient(id, BLOCK)));
                basin.addProperty("processing_time", 320);
                basin.add("result", tagResult("c:storage_blocks/" + name));
                emit.accept("metallurgy/casting_in_basin", name + suffix, basin);
            }
        }
    }


    private void melt(Emitter emit, String metal, String form, String tag, String fluid, int amount,
                      int time, int slag, String heat, LoadCondition present, Variant<String> variant,
                      String suffix) {
        List<LoadCondition> conditions = new ArrayList<>();
        conditions.add(present);
        conditions.add(hasItems(tag));
        conditions.addAll(variant.conditions());

        JsonObject json = recipe("createmetallurgy:melting", conditions);
        json.addProperty("heat_requirement", heat);
        json.add("ingredients", array(tagIngredient(tag)));
        json.addProperty("processing_time", time);
        json.add("results", slag == 0
                ? array(fluidResult(fluid, amount))
                : array(fluidResult(fluid, amount), fluidResult(METALLURGY_SLAG, slag)));
        emit.accept("metallurgy/melting", metal + "_from_" + form + suffix, json);
    }

    private static List<String> parityMetals() {
        List<String> metals = new ArrayList<>();
        for (Metal metal : Metals.ALL) {
            if (!metal.fluidOwners().contains(Metals.METALLURGY) && metal.standardForms()) {
                metals.add(metal.name());
            }
        }
        return metals;
    }

    private record Restated(String metal, String item, Metal.Heat heat) {}

    private static final List<Restated> METALLURGY_CRUSHED = List.of(
            new Restated("aluminum", "create:crushed_raw_aluminum", Metal.Heat.HEATED),
            new Restated("copper", "create:crushed_raw_copper", Metal.Heat.HEATED),
            new Restated("gold", "create:crushed_raw_gold", Metal.Heat.HEATED),
            new Restated("iron", "create:crushed_raw_iron", Metal.Heat.HEATED),
            new Restated("lead", "create:crushed_raw_lead", Metal.Heat.HEATED),
            new Restated("nickel", "create:crushed_raw_nickel", Metal.Heat.HEATED),
            new Restated("osmium", "create:crushed_raw_osmium", Metal.Heat.SUPERHEATED),
            new Restated("silver", "create:crushed_raw_silver", Metal.Heat.HEATED),
            new Restated("tin", "create:crushed_raw_tin", Metal.Heat.HEATED),
            new Restated("tungsten", "createmetallurgy:crushed_raw_tungsten", Metal.Heat.SUPERHEATED),
            new Restated("zinc", "create:crushed_raw_zinc", Metal.Heat.HEATED));

    /**
     * Metallurgy's own crushed melts, restated at this mod's doubled rate.
     *
     * <p>{@link #parityMetals} skips the eleven metals Metallurgy owns the fluid for, because it ships
     * a full melting set for them itself -- and prices a crushed ore at 90 there, where our Create
     * mixer pays 180 of that same fluid. Left alone its purpose-built melter is worth half a plain
     * basin for the commonest metals in the game, so these are rewritten in its namespace the way
     * tfmgSteel rewrites TFMG's. That override only wins because neoforge.mods.toml orders this mod
     * AFTER createmetallurgy; drop that entry and these files lose to the ones they are replacing.
     *
     * <p>Only the amount is ours. The item is Metallurgy's -- ten of them Create's crushed ores and
     * tungsten its own -- as are the 45 mB of slag, the 48 ticks, and the heat tier, which is
     * superheated for osmium and tungsten where no metal this mod registers is.
     */
    private void metallurgyCrushed(ForeignEmitter foreign) {
        List<LoadCondition> conditions = List.of(LoadCondition.modLoaded(Metals.METALLURGY));

        for (Restated subject : METALLURGY_CRUSHED) {
            JsonObject json = recipe("createmetallurgy:melting", conditions);
            json.addProperty("heat_requirement", subject.heat().json);
            json.add("ingredients", array(itemIngredient(subject.item())));
            json.addProperty("processing_time", 48);
            json.add("results", array(
                    fluidResult("createmetallurgy:molten_" + subject.metal(), CRUSHED_AMOUNT),
                    fluidResult(METALLURGY_SLAG, SLAG_RAW)));
            foreign.accept(Metals.METALLURGY, "melting/" + subject.metal() + "/raw_crushed", json);
        }
    }

    /** Metals TFMG casts itself; tfmgSteel and tfmgSilicon rewrite its recipes rather than adding to them. */
    private static final java.util.Set<String> TFMG_OWNED = java.util.Set.of("steel", "silicon");

    private void tfmgParity(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(TFMG);
        for (Metal metal : Metals.ALL) {
            if (TFMG_OWNED.contains(metal.name()) || !metal.standardForms()) continue;
            String fluidTag = "c:molten_" + metal.name();

            for (Variant<Source> source : solidVariants(metal)) {
                String ingot = source.value().ingot();
                if (ingot == null) continue;

                List<LoadCondition> conditions = new ArrayList<>();
                conditions.add(present);
                conditions.addAll(source.conditions());

                JsonObject casting = recipe("tfmg:casting", conditions);
                casting.add("ingredients", array(fluidIngredient(fluidTag, UNIT)));
                casting.addProperty("processing_time", 200);
                casting.add("results", array(itemResult(ingot)));
                emit.accept("tfmg/casting", metal.name() + source.suffix(), casting);
            }

            for (BlastForm form : BLAST_FORMS) {
                String tag = form.tagPrefix() + metal.name();
                for (Variant<String> fluid : fluidVariants(metal)) {
                    JsonObject json = recipe("tfmg:industrial_blasting", gated(present, tag, fluid));
                    json.addProperty("hot_air_usage", form.hotAir());
                    json.add("ingredients", array(tagIngredient(tag), tagIngredient(FLUX)));
                    json.addProperty("processing_time", form.time());
                    List<JsonObject> results = new ArrayList<>();
                    results.add(fluidResult(fluid.value(), form.amount()));
                    if (form.slag() > 0) results.add(fluidResult(TFMG_SLAG, form.slag()));
                    results.add(fluidResult(TFMG_GAS, form.gas()));
                    json.add("results", array(results));
                    emit.accept("tfmg/industrial_blasting", metal.name() + form.suffix() + fluid.suffix(), json);
                }
            }
        }
    }

    private record BlastForm(String tagPrefix, String suffix, int amount, int slag, int gas, int hotAir,
                             int time) {}

    /**
     * Each form pays what a heated mixer pays for it; left at UNIT, TFMG's own blasting machine would
     * be the slowest route to the least crushed-ore metal. Gas, hot air and time are TFMG's own
     * figures for that form, from the raw-iron and iron-dust steel recipes it shipped.
     *
     * <p>The blast furnace matches on the first ingredient alone and takes the first recipe that
     * matches, so the three tags must stay disjoint, and no other recipe may claim the same input --
     * which is why tfmgSteel switches TFMG's iron-to-steel blasting off.
     *
     * <p>A clean dust makes no slag. The ore hammer makes one from an ingot, and the centrifuge turns
     * slag into nuggets, so slag off a dust would be ingot in, ingot and metal out. Slag comes only
     * from ore-bound forms, as Metallurgy's own melter does it.
     */
    private static final List<BlastForm> BLAST_FORMS = List.of(
            new BlastForm("c:crushed_raw_materials/", "", CRUSHED_AMOUNT, UNIT, 200, 20, 20),
            new BlastForm("c:raw_materials/", "_from_raw", RAW_AMOUNT, UNIT / 2, 200, 40, 40),
            new BlastForm("c:dusts/", "_from_dust", UNIT, 0, 20, 20, 20));

    private static final String COKE = "tfmg:coal_coke_dust";
    private static final String FLUX = "tfmg:flux";
    private static final String ACID = "tfmg:sulfuric_acid";

    private static final String CAST_IRON_VAT = "tfmg:cast_iron_vat";
    private static final String STEEL_VAT = "tfmg:steel_vat";
    private static final String FIREPROOF_VAT = "tfmg:firebrick_lined_vat";
    private static final String[] ALL_VATS = {CAST_IRON_VAT, STEEL_VAT, FIREPROOF_VAT};
    /** The two vats TFMG runs its own electrolysis and soaking recipes in. */
    private static final String[] ELECTRIC_VATS = {STEEL_VAT, FIREPROOF_VAT};

    private static final String[] MIXER = {"tfmg:mixing"};
    private static final String[] CENTRIFUGE = {"tfmg:centrifuge"};
    private static final String[] ELECTROLYZER = {"tfmg:electrode", "tfmg:electrode"};
    private static final String[] ARC_ELECTRODES =
            {"tfmg:graphite_electrode", "tfmg:graphite_electrode", "tfmg:graphite_electrode"};
    /** No attachment at all: TFMG's soaking mode, the one its circuit-board etching uses. */
    private static final String[] NO_MACHINES = {};

    /** Steel is molten iron and coal at the Create mixer's ratio, in every machine that makes it. */
    private static final String STEEL_CARBON = "minecraft:coals";

    /** The fireproof tier's dirty-dust payout, which the arc furnace and electrolysis tie: 3x per crushed ore. */
    private static final int TFMG_DIRTY_TOP = UNIT * 12 / 5;

    private record VatTier(String name, String vat, int size, int alloySize, int cleanTime,
                           int dirtyTime, int alloyTime, int dirtyAmount) {}

    /**
     * One entry per TFMG vat, each naming only its own vat type: TFMG runs the first recipe that
     * matches, so two tiers sharing a vat would be a coin toss between them.
     *
     * <p>TFMG adds one to the timer per tick and runs one recipe per cycle -- no batching, and the
     * mixer's RPM does not scale it -- so processing_time is a vat's whole throughput, 1200 / time
     * per minute. The 300 ticks this used to take was six to twenty times slower than every other
     * melting route. A better vat is faster and smaller; a dirty dust's bonus costs double the time
     * in the same vat.
     *
     * <p>Clean dust pays UNIT in every vat, because the ore hammer makes one from an ingot. Dirty dust
     * is milled from ore and nothing makes it from an ingot, so it can climb. Through the millstone's
     * 1.25 per crushed ore, the cast iron tier's 180 ties the wheels at 2.5x and every later stage
     * pays more than they do -- steel 2.75x, fireproof 3x -- so building TFMG past Create is worth
     * it in yield, not only in slag.
     */
    private static final List<VatTier> VAT_TIERS = List.of(
            new VatTier("cast_iron", CAST_IRON_VAT, 12, 18, 100, 200, 150, CRUSHED_AMOUNT),
            new VatTier("steel", STEEL_VAT, 9, 12, 60, 120, 100, UNIT * 11 / 5),
            new VatTier("fireproof", FIREPROOF_VAT, 4, 9, 40, 80, 60, TFMG_DIRTY_TOP));

    /** Vat matching also reads the output tanks and output slots, so outputs can satisfy another recipe's inputs. */
    private static JsonObject vat(List<LoadCondition> conditions, String[] vats, String[] machines,
                                  int size, int time) {
        JsonObject vat = recipe("tfmg:vat_machine_recipe", conditions);
        vat.add("allowed_vat_types", RecipeJson.stringArray(vats));
        if (machines.length > 0) vat.add("machines", RecipeJson.stringArray(machines));
        vat.addProperty("min_size", size);
        vat.addProperty("processing_time", time);
        return vat;
    }

    private static List<LoadCondition> gated(LoadCondition present, String tag, Variant<String> fluid) {
        List<LoadCondition> conditions = new ArrayList<>();
        conditions.add(present);
        conditions.add(hasItems(tag));
        conditions.addAll(fluid.conditions());
        return conditions;
    }

    private void vatSmelting(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(TFMG);

        for (VatTier tier : VAT_TIERS) {
            String[] vatType = {tier.vat()};
            String folder = "/" + tier.name();

            for (Metal metal : Metals.ALL) {
                if (!metal.standardForms()) continue;
                String dustTag = "c:dusts/" + metal.name();
                String dirtyTag = "c:dirty_dusts/" + metal.name();

                for (Variant<String> fluid : fluidVariants(metal)) {
                    JsonObject clean = vat(gated(present, dustTag, fluid), vatType, MIXER,
                            tier.size(), tier.cleanTime());
                    clean.add("ingredients", array(tagIngredient(dustTag), itemIngredient(COKE)));
                    clean.addProperty("heat_requirement", metal.heat().json);
                    // UNIT, not twice it: an ore hammer makes a dust from an ingot, so paying two ingots
                    // for one dust was ingot -> dust -> vat -> two ingots, a duplicator needing only a
                    // crafting table. No slag either, for the same reason: the centrifuge turns slag
                    // into nuggets, so slag off a dust would be ingot in, ingot and metal out for a
                    // tenth of a coke. Slag comes from the dirty dust below, which only ore makes.
                    clean.add("results", array(
                            chanceResult(COKE, 0.9),
                            fluidResult(fluid.value(), UNIT)));
                    emit.accept("tfmg/vat_smelting" + folder, metal.name() + fluid.suffix(), clean);

                    JsonObject dirty = vat(gated(present, dirtyTag, fluid), vatType, MIXER,
                            tier.size(), tier.dirtyTime());
                    dirty.add("ingredients", array(tagIngredient(dirtyTag), itemIngredient(COKE)));
                    dirty.addProperty("heat_requirement", metal.heat().json);
                    dirty.add("results", array(
                            chanceResult(COKE, 0.9),
                            fluidResult(fluid.value(), tier.dirtyAmount()),
                            fluidResult(TFMG_SLAG, UNIT * 2)));
                    emit.accept("tfmg/vat_smelting_dirty" + folder, metal.name() + fluid.suffix(), dirty);
                }
            }

            for (Alloy a : ALLOYS) {
                List<JsonObject> ingredients = alloyIngredients(a);
                List<LoadCondition> shared = new ArrayList<>();
                shared.add(present);
                if (a.requires() != null) shared.add(LoadCondition.modLoaded(a.requires()));

                for (Variant<String> fluid : fluidVariants(byName(a.metal()))) {
                    List<LoadCondition> conditions = new ArrayList<>(shared);
                    conditions.addAll(fluid.conditions());

                    JsonObject vat = vat(conditions, vatType, MIXER, tier.alloySize(), tier.alloyTime());
                    vat.add("ingredients", array(ingredients));
                    vat.addProperty("heat_requirement", a.heat().json);
                    vat.add("results", array(fluidResult(fluid.value(), a.amount())));
                    emit.accept("tfmg/vat_alloying" + folder,
                            "molten_" + a.metal() + fluid.suffix() + a.variant(), vat);
                }
            }

            // molten_steel_from_molten_iron's recipe, so the vat makes steel the way the mixer does.
            // Coal rather than coke: every vat smelt hands its coke back into the output slots, which
            // vat matching reads, so coke here would turn the vat's own iron into steel unasked.
            for (Variant<String> fluid : fluidVariants(byName("steel"))) {
                List<LoadCondition> conditions = new ArrayList<>();
                conditions.add(present);
                conditions.addAll(fluid.conditions());

                JsonObject vat = vat(conditions, vatType, MIXER, tier.alloySize(), tier.alloyTime());
                vat.add("ingredients", array(
                        fluidIngredient("c:molten_iron", UNIT), tagIngredient(STEEL_CARBON)));
                vat.addProperty("heat_requirement", Metal.Heat.HEATED.json);
                vat.add("results", array(fluidResult(fluid.value(), UNIT * 2)));
                emit.accept("tfmg/vat_alloying" + folder, "molten_steel_from_molten_iron" + fluid.suffix(), vat);
            }
        }
    }

    /**
     * The arc furnace -- three graphite electrodes on a fireproof vat -- takes ore-bound forms only:
     * dirty dust at the fireproof tier's rate, and raw ore at the mixer's RAW_AMOUNT, since crushing
     * is what pays double and no machine may pay crushed rates for raw ore. It never makes steel;
     * {@link #tfmgSteel} restates TFMG's own arc recipe as the iron-to-steel converter.
     */
    private void vatArcFurnace(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(TFMG);
        String[] fireproof = {FIREPROOF_VAT};

        for (Metal metal : Metals.ALL) {
            if (TFMG_OWNED.contains(metal.name()) || !metal.standardForms()) continue;
            String dirtyTag = "c:dirty_dusts/" + metal.name();
            String rawTag = "c:raw_materials/" + metal.name();

            for (Variant<String> fluid : fluidVariants(metal)) {
                JsonObject dirty = vat(gated(present, dirtyTag, fluid), fireproof, ARC_ELECTRODES, 9, 40);
                dirty.add("ingredients", array(tagIngredient(dirtyTag), tagIngredient(FLUX), itemIngredient(COKE)));
                dirty.add("results", array(
                        chanceResult(COKE, 0.9),
                        fluidResult(fluid.value(), TFMG_DIRTY_TOP),
                        fluidResult(TFMG_SLAG, UNIT * 2)));
                emit.accept("tfmg/arc_furnace", metal.name() + fluid.suffix(), dirty);

                JsonObject raw = vat(gated(present, rawTag, fluid), fireproof, ARC_ELECTRODES, 9, 40);
                raw.add("ingredients", array(tagIngredient(rawTag), tagIngredient(FLUX), itemIngredient(COKE)));
                raw.add("results", array(
                        chanceResult(COKE, 0.9),
                        fluidResult(fluid.value(), RAW_AMOUNT),
                        fluidResult(TFMG_SLAG, UNIT)));
                emit.accept("tfmg/arc_furnace", metal.name() + "_from_raw" + fluid.suffix(), raw);
            }
        }
    }

    /** Smelted electrolytically in the real world, as TFMG's own aluminum is from bauxite. */
    private static final java.util.Set<String> ELECTROLYTIC = java.util.Set.of("aluminum", "lithium");

    /** Ties the arc on dirty dust, needing no flux, coke or slag handling and a single vat block. */
    private void vatElectrolysis(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(TFMG);

        for (String name : ELECTROLYTIC) {
            String dirtyTag = "c:dirty_dusts/" + name;
            for (Variant<String> fluid : fluidVariants(byName(name))) {
                JsonObject vat = vat(gated(present, dirtyTag, fluid), ELECTRIC_VATS, ELECTROLYZER, 1, 100);
                vat.add("ingredients", array(tagIngredient(dirtyTag)));
                vat.addProperty("heat_requirement", Metal.Heat.HEATED.json);
                vat.add("results", array(fluidResult(fluid.value(), TFMG_DIRTY_TOP)));
                emit.accept("tfmg/electrolysis", name + fluid.suffix(), vat);
            }
        }
    }

    /**
     * The top of the yield ladder: 252 per dirty dust is 315 per crushed ore, 3.5 ingots. It needs
     * no attachment, so vats parallelise cheaply and the sulfuric acid is what gates it, not the 300
     * ticks. The only other recipe for a vat with no machine on it is TFMG's circuit-board etching,
     * which wants a coated board rather than a dust, so it cannot shadow the routes below.
     */
    private void vatLeaching(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(TFMG);

        for (Metal metal : Metals.ALL) {
            if (TFMG_OWNED.contains(metal.name()) || !metal.standardForms()) continue;
            String dirtyTag = "c:dirty_dusts/" + metal.name();

            for (Variant<String> fluid : fluidVariants(metal)) {
                JsonObject vat = vat(gated(present, dirtyTag, fluid), ELECTRIC_VATS, NO_MACHINES, 1, 300);
                vat.add("ingredients", array(tagIngredient(dirtyTag), concreteFluidIngredient(ACID, 250)));
                vat.add("results", array(fluidResult(fluid.value(), UNIT * 14 / 5)));
                emit.accept("tfmg/leaching", metal.name() + fluid.suffix(), vat);
            }
        }
    }

    /**
     * Simple Ore Doubling's slag washing, done on the molten slag directly: one slag's worth at the one
     * slag rate pays what washing that slag would, less the experience. See slagSolids.
     */
    private void vatSlagRecovery(Emitter emit) {
        JsonObject vat = vat(List.of(LoadCondition.modLoaded(TFMG)), ALL_VATS, CENTRIFUGE, 1, 100);
        vat.add("ingredients", array(fluidIngredient(SLAG_TAG, UNIT)));
        vat.add("results", array(
                chanceResult("minecraft:iron_nugget", 0.075),
                chanceResult("create:copper_nugget", 0.075),
                chanceResult("minecraft:gold_nugget", 0.075),
                chanceResult("create:zinc_nugget", 0.075)));
        emit.accept("tfmg/centrifuge", "slag_recovery", vat);
    }

    private void bigCannonsParity(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(Metals.BIG_CANNONS);

        for (Metal metal : Metals.ALL) {
            if (!metal.standardForms()) continue;
            if (metal.fluidOwners().contains(Metals.BIG_CANNONS)) continue;

            for (Variant<String> fluid : fluidVariants(metal)) {
                cannonMelt(emit, metal, "nugget", "c:nuggets/", NUGGET, 20, present, fluid);
                cannonMelt(emit, metal, "ingot", "c:ingots/", UNIT, 180, present, fluid);
                cannonMelt(emit, metal, "block", "c:storage_blocks/", BLOCK, 1620, present, fluid);
            }
        }
    }

    private void cannonMelt(Emitter emit, Metal metal, String form, String tagPrefix, int amount,
                            int time, LoadCondition present, Variant<String> fluid) {
        String tag = tagPrefix + metal.name();
        List<LoadCondition> conditions = new ArrayList<>();
        conditions.add(present);
        conditions.add(hasItems(tag));
        conditions.addAll(fluid.conditions());

        JsonObject json = recipe("createbigcannons:melting", conditions);
        json.addProperty("heat_requirement", metal.heat().json);
        json.add("ingredients", array(tagIngredient(tag)));
        json.addProperty("processing_time", time);
        json.add("results", array(fluidResult(fluid.value(), amount)));
        emit.accept("bigcannons/melting", metal.name() + "_" + form + fluid.suffix(), json);
    }

    private static final int TFMG_UNIT = 144;

    /**
     * TFMG's steel recipes, fitted to this mod's process: ore always melts to iron, and steel is
     * molten iron and coal at the Create mixer's ratio, so iron behaves the same with TFMG or without.
     *
     * <p>TFMG turned crushed iron, raw iron and iron dust straight into steel in its blast furnace.
     * Those three are switched off; tfmgParity blasts every metal's crushed, raw and dust forms into
     * its molten form instead, iron included. Left on, crushed iron matched both TFMG's steel and our
     * iron, and which one a pack got came down to recipe-id hash order.
     *
     * <p>The arc furnace keeps its id but becomes the converter: 90 molten iron and a coal into 180
     * steel, as molten_steel_from_molten_iron does in a basin. Coal, not coke, because the arc's own
     * ore recipes return coke into the output slots that vat matching reads.
     */
    private void tfmgSteel(ForeignEmitter foreign) {
        List<LoadCondition> conditions = List.of(LoadCondition.modLoaded(TFMG));

        JsonObject casting = recipe("tfmg:casting", conditions);
        casting.add("ingredients", array(concreteFluidIngredient(TFMG_STEEL, UNIT)));
        casting.addProperty("processing_time", 200);
        casting.add("results", array(itemResult("tfmg:steel_ingot")));
        foreign.accept(TFMG, "casting/steel", casting);

        for (String name : List.of("steel", "steel_from_dust", "steel_from_raw_iron")) {
            foreign.accept(TFMG, "industrial_blasting/" + name, switchedOff(TFMG, "tfmg:industrial_blasting"));
        }

        JsonObject vat = vat(conditions, new String[] {FIREPROOF_VAT}, ARC_ELECTRODES, 9, 40);
        vat.add("ingredients", array(fluidIngredient("c:molten_iron", UNIT), tagIngredient(STEEL_CARBON)));
        vat.add("results", array(fluidResult(TFMG_STEEL, UNIT * 2)));
        foreign.accept(TFMG, "vat_machine_recipe/arc_furnace_steel", vat);
    }

    /**
     * Overrides another mod's recipe by id with one that never loads. Present AND absent is false on
     * both loaders; "absent" alone would load wherever the mod is missing and fail to parse a recipe
     * type nothing registered.
     */
    private static JsonObject switchedOff(String modId, String type) {
        return recipe(type, List.of(LoadCondition.modLoaded(modId), LoadCondition.modAbsent(modId)));
    }

    /**
     * TFMG's two silicon recipes, restated at this mod's unit.
     *
     * <p>TFMG values a silicon ingot at 144 and blasts quartz into 40. Left alone beside a 90 mB cast
     * that is a silicon buff, and molten magnetic alloy would be mixing two units, so both are rewritten
     * in TFMG's own namespace at the same three-and-a-bit quartz to the ingot they already cost.
     */
    private void tfmgSilicon(ForeignEmitter foreign) {
        List<LoadCondition> conditions = List.of(LoadCondition.modLoaded(TFMG));

        JsonObject casting = recipe("tfmg:casting", conditions);
        casting.add("ingredients", array(concreteFluidIngredient(TFMG_SILICON, UNIT)));
        casting.addProperty("processing_time", 200);
        casting.add("results", array(itemResult("tfmg:silicon_ingot")));
        foreign.accept(TFMG, "casting/silicon", casting);

        JsonObject blasting = recipe("tfmg:industrial_blasting", conditions);
        blasting.add("ingredients", array(itemIngredient("minecraft:quartz")));
        blasting.addProperty("processing_time", 5);
        blasting.add("results", array(fluidResult(TFMG_SILICON, 40 * UNIT / TFMG_UNIT)));
        foreign.accept(TFMG, "industrial_blasting/silicon", blasting);
    }

    private static List<Variant<String>> fluidVariants(Metal metal) {
        List<String> ids = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        for (String owner : metal.fluidOwners()) {
            ids.add(Metal.fluidOf(owner, metal.name()));
            owners.add(Metal.ownerOf(owner));
        }
        // Naming an unregistered fluid emits a recipe that fails to parse.
        if (DECLARED_FLUIDS.contains(metal.name())) {
            ids.add("createmetalwork:molten_" + metal.name());
            owners.add(null);
        }
        return own(Variant.exclusive(ids, owners), "molten_" + metal.name());
    }

    /**
     * Gates the variant this mod owns on its config entry still being there.
     *
     * <p>Only the unconditional variant names a {@code createmetalwork:} id; the rest defer to a mod
     * that owns the fluid itself. Tag-based recipes need no gate -- those tags are emitted optional,
     * so a fluid the user removed leaves an empty tag rather than a broken one.
     */
    private static <T> List<Variant<T>> own(List<Variant<T>> variants, String... ids) {
        List<Variant<T>> gated = new ArrayList<>(variants.size());
        for (Variant<T> variant : variants) {
            if (!variant.suffix().isEmpty()) {
                gated.add(variant);
                continue;
            }
            List<LoadCondition> conditions = new ArrayList<>(variant.conditions());
            conditions.add(enabled(ids));
            gated.add(new Variant<>(variant.value(), List.copyOf(conditions), variant.suffix()));
        }
        return gated;
    }

    private static final java.util.Set<String> DECLARED_FLUIDS = declaredFluids();

    private static java.util.Set<String> declaredFluids() {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (String entry : dev.averageanime.createmetalwork.config.ConfigDefaults.CUSTOM_FLUID_DEFAULT) {
            String id = entry.split("@")[0].split(Pattern.quote("|"))[0];
            if (id.startsWith("molten_")) names.add(id.substring("molten_".length()));
        }
        return names;
    }

    private static List<Variant<Source>> solidVariants(Metal metal) {
        List<Source> sources = metal.reachableSources();
        List<String> owners = new ArrayList<>();
        for (Source source : sources) owners.add(source.modId());
        return Variant.exclusive(sources, owners);
    }

    private static Metal byName(String name) {
        for (Metal metal : Metals.ALL) {
            if (metal.name().equals(name)) return metal;
        }
        throw new IllegalArgumentException("No metal named " + name + " in Metals.ALL");
    }
}
