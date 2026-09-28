package dev.averageanime.createmetalwork.neoforge.datagen;

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
    private static final String TFMG_GAS = "tfmg:furnace_gas";
    private static final String TFMG_SILICON = "tfmg:liquid_silicon";

    private static final int RAW_AMOUNT = UNIT;

    private static final int CRUSHED_AMOUNT = UNIT;

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
        }
        andesite(emit);
        alloys(emit);
        crushing(emit);
        oreDoubling(emit);
        dustChain(emit);
        tfmgSteel(foreign);
        tfmgSilicon(foreign);
        metallurgyParity(emit);
        tfmgParity(emit);
        fantasyAlloys(emit);
        collapsedAlloys(emit);
        vatSmelting(emit);
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
        alloy(emit, "molten_brass_from_molten", "brass", Metal.Heat.HEATED, UNIT,
                List.of(fluidIngredient("c:molten_copper", UNIT), fluidIngredient("c:molten_zinc", UNIT)),
                List.of());

        alloy(emit, "molten_brass_from_crushed_raw", "brass", Metal.Heat.HEATED, CRUSHED_AMOUNT,
                List.of(tagIngredient("c:crushed_raw_materials/copper"),
                        tagIngredient("c:crushed_raw_materials/zinc")),
                List.of());

        for (String metal : List.of("iron", "zinc")) {
            alloy(emit, "molten_andesite_alloy_from_molten_" + metal, "andesite_alloy",
                    Metal.Heat.HEATED, UNIT,
                    List.of(fluidIngredient("c:molten_andesite", UNIT),
                            fluidIngredient("c:molten_" + metal, NUGGET)),
                    List.of());
        }

        alloy(emit, "molten_netherite_from_molten_gold", "netherite", Metal.Heat.HEATED, UNIT,
                List.of(fluidIngredient("c:molten_gold", UNIT * 4),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap"),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap"),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap"),
                        tagIngredient("c:crushed_raw_materials/netherite_scrap")),
                List.of());

        // Scrap is not netherite, so its dust gets no melt of its own; it feeds the same alloy the
        // crushed form does, at the same four-to-an-ingot rate.
        alloy(emit, "molten_netherite_from_molten_gold_and_dust", "netherite", Metal.Heat.HEATED, UNIT,
                List.of(fluidIngredient("c:molten_gold", UNIT * 4),
                        tagIngredient("c:dusts/netherite_scrap"),
                        tagIngredient("c:dusts/netherite_scrap"),
                        tagIngredient("c:dusts/netherite_scrap"),
                        tagIngredient("c:dusts/netherite_scrap")),
                List.of(hasItems("c:dusts/netherite_scrap")));

        alloy(emit, "molten_steel_from_molten_iron", "steel", Metal.Heat.HEATED, UNIT,
                List.of(fluidIngredient("c:molten_iron", UNIT), tagIngredient("minecraft:coals")),
                List.of());

        // Iron and coal again, which is also what steel is made of just above. Big Cannons separates its
        // own two by machine rather than by ingredient -- compacting presses iron into cast iron, mixing
        // stirs it into steel -- so this follows suit. As create:mixing it would be an ambiguous basin.
        mix(emit, "create:compacting", "compacting/alloying", "molten_cast_iron_from_molten_iron",
                "cast_iron", Metal.Heat.HEATED, UNIT,
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

        JsonObject scrap = recipe("create:crushing",
                List.of(CREATE, enabled("crushed_netherite_scrap")));
        scrap.add("ingredients", array(itemIngredient("minecraft:netherite_scrap")));
        scrap.add("results", array(
                itemResult("createmetalwork:crushed_netherite_scrap"),
                chanceResult("createmetalwork:crushed_netherite_scrap", 0.5),
                itemResult("create:experience_nugget"),
                chanceResult("create:experience_nugget", 0.5)));
        scrap.addProperty("processing_time", 700);
        emit.accept("crushing", "crushed_netherite_scrap_from_scrap", scrap);
    }

    // unit is how many of the crushed item make an ingot's worth: nine for andesite, whose crushed form
    // melts at a ninth, one everywhere else. Doubling multiplies that, not the item count.
    private record Doubling(String name, JsonObject input, String crushed, int unit,
                            List<JsonObject> bonus, int processingTime) {}

    private static final String SOD = "create_simple_ore_doubling";

    private static final int LAVA = 50;

    private void oreDoubling(Emitter emit) {
        List<Doubling> subjects = List.of(
                new Doubling("crushed_andesite", itemIngredient("minecraft:andesite"),
                        "createmetalwork:crushed_andesite", 9, List.of(), 0),
                new Doubling("crushed_netherite_scrap", itemIngredient("minecraft:netherite_scrap"),
                        "createmetalwork:crushed_netherite_scrap", 1,
                        List.of(chanceResult("createmetalwork:crushed_netherite_scrap", 0.5),
                                itemResult(SOD + ":slag")),
                        700),
                new Doubling("crushed_raw_tin", tagIngredient("c:raw_materials/tin"),
                        "create:crushed_raw_tin", 1, List.of(), 0));

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

    // The washed byproduct each dirty dust gives up, per metal. A trailing "*n" means that many.
    private record Dust(String metal, String washed, List<String> owners) {}

    private static Dust dust(String metal, String washed, String... owners) {
        List<String> all = new ArrayList<>(Arrays.asList(owners));
        all.add(null);
        return new Dust(metal, washed, all);
    }

    private static Dust foreignDust(String metal, String washed, String... owners) {
        return new Dust(metal, washed, Arrays.asList(owners));
    }

    private static final String CLAY = "minecraft:clay_ball";
    private static final String QUARTZ = "minecraft:quartz";
    private static final String REDSTONE = "minecraft:redstone";
    private static final String GUNPOWDER = "minecraft:gunpowder";

    private static final List<Dust> DUSTS = List.of(
            dust("adamantite", "create:powdered_obsidian"),
            dust("andesite", CLAY),
            dust("aquarium", QUARTZ),
            dust("banglum", QUARTZ),
            dust("carmot", QUARTZ),
            dust("cincinnasite", QUARTZ),
            dust("kyber", QUARTZ),
            dust("lead", "create:zinc_nugget*2", Metals.ATO),
            dust("lithium", GUNPOWDER),
            dust("manganese", QUARTZ),
            dust("midas_gold", QUARTZ),
            dust("mythril", QUARTZ),
            dust("netherite_scrap", "minecraft:gold_nugget*2"),
            dust("nickel", CLAY, Metals.ATO),
            dust("orichalcum", QUARTZ),
            dust("palladium", QUARTZ),
            dust("prometheum", QUARTZ),
            dust("quadrillum", QUARTZ),
            dust("runite", QUARTZ),
            dust("starrite", QUARTZ),
            dust("stormyx", QUARTZ),
            dust("thallasium", QUARTZ),
            dust("tin", REDSTONE, Metals.ATO),
            dust("tungsten", "create:zinc_nugget*2", Metals.METALLURGY),
            // No unobtainium here: it is a crushed-only material with no molten form, so it has no
            // dirty dust for the chain to mill into.
            foreignDust("copper", CLAY, Metals.METALLURGY, Metals.ATO),
            foreignDust("gold", QUARTZ, Metals.METALLURGY, Metals.ATO),
            foreignDust("iron", REDSTONE, Metals.METALLURGY, Metals.ATO),
            foreignDust("zinc", GUNPOWDER, Metals.METALLURGY, Metals.ATO),
            foreignDust("aluminum", CLAY, Metals.ATO),
            foreignDust("osmium", CLAY, Metals.ATO),
            foreignDust("platinum", CLAY, Metals.ATO),
            foreignDust("silver", CLAY, Metals.ATO),
            foreignDust("uranium", CLAY, Metals.ATO));

    private static final int GRIND_TIME = 150;

    private static final double GRIND_BONUS = 0.25;

    private void dustChain(Emitter emit) {
        for (Dust dust : DUSTS) {
            String crushedTag = "c:crushed_raw_materials/" + dust.metal();
            String dirtyTag = "c:dirty_dusts/" + dust.metal();

            for (Variant<String> owner : dustVariants(dust)) {
                String clean = dustId(owner.value(), dust.metal(), false);
                String dirty = dustId(owner.value(), dust.metal(), true);

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

                JsonObject splashing = recipe("create:splashing", wash);
                splashing.add("ingredients", array(tagIngredient(dirtyTag)));
                splashing.add("results", array(itemResult(clean), washBonus(dust.washed())));
                emit.accept("splashing", dust.metal() + "_dust_from_dirty" + owner.suffix(), splashing);
            }
        }
    }

    private static JsonObject washBonus(String washed) {
        int star = washed.indexOf('*');
        return star < 0
                ? chanceResult(washed, WASH_BONUS)
                : chanceResult(washed.substring(0, star), WASH_BONUS,
                        Integer.parseInt(washed.substring(star + 1)));
    }

    private static String dustId(String owner, String metal, boolean dirty) {
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

    private static final List<Alloy> ALLOYS = List.of(
            alloy("bronze", 360, Metal.Heat.HEATED, null, "270:copper 90:tin"),
            alloy("aeternium", 180, Metal.Heat.HEATED, "betterend", "90:thallasium 90:netherite"),
            alloy("celestium", 90, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:star_platinum 90:kyrmot",
                    "#c:crushed_unobtainium"),
            alloy("durasteel", 360, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:quadrillum 90:manganese"),
            alloy("hallowed", 90, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:mythantite 90:orichalcum"),
            alloy("kyrmot", 90, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:kyber 90:carmot"),
            alloy("metallurgium", 90, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
                    "90:mythantite 90:orichadium", "#c:crushed_unobtainium"),
            alloy("mythantite", 90, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:mythril 90:adamantite"),
            alloy("orichadium", 90, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:orichalcum 90:palladium"),
            alloy("star_platinum", 90, Metal.Heat.HEATED, Metals.MYTHIC_METALS, "90:starrite 90:platinum"),
            alloy("terminite", 180, Metal.Heat.HEATED, "betterend", "90:iron", "betterend:ender_dust"),
            alloy("steel", UNIT, Metal.Heat.HEATED, Metals.NORTHSTAR, "180:iron",
                    "northstar:volcanic_ash"),
            // TFMG mixes two nickel, one silicon and two steel into two magnetic alloy ingots. The same
            // five-in, two-out ratio, in the melt.
            alloy("magnetic_alloy", UNIT * 2, Metal.Heat.HEATED, TFMG,
                    "180:nickel 90:silicon 180:steel"),

            // Alloys the mods that own the metal only ever make out of solids. Ratios are theirs.
            // All The Ores blends a gold and a silver dust into two electrum.
            alloy("electrum", UNIT * 2, Metal.Heat.HEATED, null, "90:gold 90:silver"),
            // All The Ores: three lead, one platinum and two ender pearls make four enderium.
            alloy("enderium", UNIT * 4, Metal.Heat.HEATED, null, "270:lead 90:platinum",
                    "minecraft:ender_pearl", "minecraft:ender_pearl"),
            // Big Cannons superheats a netherite scrap with four steel into eight nethersteel, or with
            // eight cast iron for the same eight. Both, since cast iron is the way in for anyone who has
            // not got to steel yet.
            alloy("nethersteel", UNIT * 8, Metal.Heat.SUPERHEATED, null, "360:steel",
                    "minecraft:netherite_scrap"),
            alloy("nethersteel", "_alt", UNIT * 8, Metal.Heat.SUPERHEATED, null, "720:cast_iron",
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

    private static final List<Alloy> COLLAPSED = List.of(
            alloy("hallowed", UNIT, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
                    "90:mythril 90:adamantite 90:orichalcum"),
            alloy("metallurgium", UNIT, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
                    "90:mythril 90:adamantite 90:orichalcum 90:palladium", "#c:crushed_unobtainium"),
            alloy("celestium", UNIT, Metal.Heat.HEATED, Metals.MYTHIC_METALS,
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

                melt(emit, name, "nugget", "c:nuggets/" + name, id, NUGGET, present, fluid, suffix);
                melt(emit, name, "ingot", "c:ingots/" + name, id, UNIT, present, fluid, suffix);
                melt(emit, name, "dust", "c:dusts/" + name, id, UNIT, present, fluid, suffix);
                melt(emit, name, "raw", "c:raw_materials/" + name, id, RAW_AMOUNT, present, fluid, suffix);

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
                      LoadCondition present, Variant<String> variant, String suffix) {
        List<LoadCondition> conditions = new ArrayList<>();
        conditions.add(present);
        conditions.add(hasItems(tag));
        conditions.addAll(variant.conditions());

        JsonObject json = recipe("createmetallurgy:melting", conditions);
        json.add("ingredients", array(tagIngredient(tag)));
        json.addProperty("processing_time", 60);
        json.add("results", array(fluidResult(fluid, amount)));
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

    /** Metals TFMG casts itself; tfmgSteel and tfmgSilicon restate its recipes rather than adding to them. */
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

            List<LoadCondition> blast = List.of(
                    present, hasItems("c:crushed_raw_materials/" + metal.name()));
            for (Variant<String> fluid : fluidVariants(metal)) {
                List<LoadCondition> conditions = new ArrayList<>(blast);
                conditions.addAll(fluid.conditions());

                JsonObject json = recipe("tfmg:industrial_blasting", conditions);
                json.addProperty("hot_air_usage", 20);
                json.add("ingredients", array(
                        tagIngredient("c:crushed_raw_materials/" + metal.name()),
                        tagIngredient("tfmg:flux")));
                json.addProperty("processing_time", 20);
                json.add("results", array(
                        fluidResult(fluid.value(), UNIT),
                        fluidResult(TFMG_SLAG, UNIT),
                        fluidResult(TFMG_GAS, 200)));
                emit.accept("tfmg/industrial_blasting", metal.name() + fluid.suffix(), json);
            }
        }
    }

    private static final String COKE = "tfmg:coal_coke_dust";

    private void vatSmelting(Emitter emit) {
        LoadCondition present = LoadCondition.modLoaded(TFMG);

        for (Metal metal : Metals.ALL) {
            if (!metal.standardForms()) continue;
            String dustTag = "c:dusts/" + metal.name();

            for (Variant<String> fluid : fluidVariants(metal)) {
                List<LoadCondition> conditions = new ArrayList<>();
                conditions.add(present);
                conditions.add(hasItems(dustTag));
                conditions.addAll(fluid.conditions());

                JsonObject vat = recipe("tfmg:vat_machine_recipe", conditions);
                vat.add("allowed_vat_types", RecipeJson.stringArray("tfmg:cast_iron_vat"));
                vat.add("ingredients", array(tagIngredient(dustTag), itemIngredient(COKE)));
                vat.add("machines", RecipeJson.stringArray("tfmg:mixing"));
                vat.addProperty("min_size", 12);
                vat.addProperty("processing_time", 300);
                vat.addProperty("heat_requirement", metal.heat().json);
                vat.add("results", array(
                        chanceResult(COKE, 0.9),
                        fluidResult(fluid.value(), UNIT * 2),
                        fluidResult(TFMG_SLAG, UNIT * 2)));
                emit.accept("tfmg/vat_smelting", metal.name() + fluid.suffix(), vat);
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

                JsonObject vat = recipe("tfmg:vat_machine_recipe", conditions);
                vat.add("allowed_vat_types", RecipeJson.stringArray("tfmg:cast_iron_vat"));
                vat.add("ingredients", array(ingredients));
                vat.add("machines", RecipeJson.stringArray("tfmg:mixing"));
                vat.addProperty("min_size", 18);
                vat.addProperty("processing_time", 350);
                vat.addProperty("heat_requirement", a.heat().json);
                vat.add("results", array(fluidResult(fluid.value(), a.amount())));
                emit.accept("tfmg/vat_alloying", "molten_" + a.metal() + fluid.suffix() + a.variant(), vat);
            }
        }
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

    private void tfmgSteel(ForeignEmitter foreign) {
        List<LoadCondition> conditions = List.of(LoadCondition.modLoaded(TFMG));

        JsonObject casting = recipe("tfmg:casting", conditions);
        casting.add("ingredients", array(concreteFluidIngredient(TFMG_STEEL, UNIT)));
        casting.addProperty("processing_time", 200);
        casting.add("results", array(itemResult("tfmg:steel_ingot")));
        foreign.accept(TFMG, "casting/steel", casting);

        blasting(foreign, "steel", itemIngredient("create:crushed_raw_iron"), UNIT, 144, 200, 20, 20);
        blasting(foreign, "steel_from_dust", tagIngredient("c:dusts/iron"), UNIT, 144, 20, 20, 20);
        blasting(foreign, "steel_from_raw_iron", itemIngredient("minecraft:raw_iron"),
                UNIT * 2, 288, 200, 40, 40);

        JsonObject vat = recipe("tfmg:vat_machine_recipe", conditions);
        vat.add("allowed_vat_types", RecipeJson.stringArray("tfmg:firebrick_lined_vat"));
        vat.add("ingredients", array(itemIngredient("create:crushed_raw_iron"),
                tagIngredient("tfmg:flux"), itemIngredient("tfmg:coal_coke_dust")));
        vat.add("machines", RecipeJson.stringArray(
                "tfmg:graphite_electrode", "tfmg:graphite_electrode", "tfmg:graphite_electrode"));
        vat.addProperty("min_size", 9);
        vat.addProperty("processing_time", 20);
        vat.add("results", array(
                chanceResult("tfmg:coal_coke_dust", 0.9),
                fluidResult(TFMG_STEEL, UNIT),
                fluidResult(TFMG_SLAG, 288)));
        foreign.accept(TFMG, "vat_machine_recipe/arc_furnace_steel", vat);
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

    private void blasting(ForeignEmitter foreign, String name, JsonObject input, int steel, int slag,
                          int gas, int hotAir, int time) {
        JsonObject json = recipe("tfmg:industrial_blasting", List.of(LoadCondition.modLoaded(TFMG)));
        json.addProperty("hot_air_usage", hotAir);
        json.add("ingredients", array(input, tagIngredient("tfmg:flux")));
        json.addProperty("processing_time", time);
        json.add("results", array(
                fluidResult(TFMG_STEEL, steel), fluidResult(TFMG_SLAG, slag), fluidResult(TFMG_GAS, gas)));
        foreign.accept(TFMG, "industrial_blasting/" + name, json);
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
