package dev.averageanime.createmetalwork.neoforge.datagen;

import dev.averageanime.createmetalwork.neoforge.datagen.Metal.Heat;
import dev.averageanime.createmetalwork.neoforge.datagen.Metal.Source;

import java.util.List;

public final class Metals {

    public static final String ATO = "alltheores";
    public static final String IRONWORKS = "create_ironworks";
    public static final String METALLURGY = "createmetallurgy";
    public static final String TFMG = "tfmg";
    public static final String MYTHIC_METALS = "mythicmetals";
    public static final String NORTHSTAR = "northstar";
    public static final String BIG_CANNONS = "createbigcannons";
    public static final String MEKANISM = "mekanism";
    public static final String BETTER_END = "betterend";
    public static final String BETTER_NETHER = "betternether";

    private static final List<String> SHARED = List.of(METALLURGY, ATO);

    public static final List<Metal> ALL = List.of(
            // Vanilla and Create metals; only the fluid can collide.
            metal("iron", Heat.HEATED, SHARED,
                    Source.always("minecraft:iron_ingot", "minecraft:iron_nugget", "minecraft:iron_block")),
            metal("gold", Heat.HEATED, SHARED,
                    Source.always("minecraft:gold_ingot", "minecraft:gold_nugget", "minecraft:gold_block")),
            // Vanilla has no copper nugget.
            metal("copper", Heat.HEATED, SHARED,
                    Source.always("minecraft:copper_ingot", null, "minecraft:copper_block")),
            metal("netherite", Heat.HEATED, SHARED,
                    Source.always("minecraft:netherite_ingot", null, "minecraft:netherite_block")),
            metal("zinc", Heat.HEATED, SHARED,
                    Source.always("create:zinc_ingot", "create:zinc_nugget", "create:zinc_block")),
            metal("brass", Heat.HEATED, SHARED,
                    Source.always("create:brass_ingot", "create:brass_nugget", "create:brass_block")),

            metal("andesite_alloy", Heat.HEATED, List.of(),
                    Source.always("create:andesite_alloy", null, "create:andesite_alloy_block")),
            // Not a metal: andesite casts back to a stone block, so no solid forms.
            special("andesite", Heat.HEATED, Source.always(null, null, null)),

            // Shared with Create: Ironworks, which wins when present; ATO is the fallback. The sources after
            // ATO are further fallbacks: each of these fluids registers precisely when ATO is absent, so
            // without them a pack whose only tin is Mekanism's or Mythic Metals' melted it with no way
            // back. The same holds for every fallback below.
            metal("tin", Heat.HEATED, SHARED,
                    Source.from(IRONWORKS, "create_ironworks:tin_ingot", null, "create_ironworks:tin_block"),
                    Source.standard(ATO, "tin"),
                    mekanism("tin"),
                    Source.standard(MYTHIC_METALS, "tin")),
            // TFMG and Big Cannons ship c:molten_steel too. TFMG values an ingot at 144; tfmgSteel restates
            // its casting at 90 in its own namespace, switches off its three iron-to-steel blasting
            // recipes so ore melts to iron as everywhere else, and turns its arc furnace recipe into
            // the iron-and-coal converter molten_steel_from_molten_iron already is in a basin. TFMG's ingot
            // here only adds a Create compacting cast; tfmgParity still skips steel through TFMG_OWNED.
            // Big Cannons calls its steel and bronze nuggets scrap.
            metal("steel", Heat.HEATED, List.of(METALLURGY, ATO, TFMG, BIG_CANNONS),
                    Source.from(IRONWORKS, "create_ironworks:steel_ingot", null, "create_ironworks:steel_block"),
                    Source.standard(ATO, "steel"),
                    mekanism("steel"),
                    Source.standard(TFMG, "steel"),
                    Source.from(BIG_CANNONS, "createbigcannons:steel_ingot", "createbigcannons:steel_scrap",
                            "createbigcannons:steel_block"),
                    Source.standard(MYTHIC_METALS, "steel")),
            metal("bronze", Heat.HEATED, List.of(METALLURGY, ATO, BIG_CANNONS),
                    Source.from(IRONWORKS, "create_ironworks:bronze_ingot", null, "create_ironworks:bronze_block"),
                    Source.standard(ATO, "bronze"),
                    mekanism("bronze"),
                    Source.from(BIG_CANNONS, "createbigcannons:bronze_ingot", "createbigcannons:bronze_scrap",
                            "createbigcannons:bronze_block"),
                    Source.standard(MYTHIC_METALS, "bronze")),

            metal("aluminum", Heat.HEATED, SHARED, Source.standard(ATO, "aluminum"),
                    Source.standard(TFMG, "aluminum")),
            metal("electrum", Heat.HEATED, SHARED, Source.standard(ATO, "electrum")),
            metal("enderium", Heat.HEATED, List.of(ATO), Source.standard(ATO, "enderium")),
            // ATO registers these fluids and ships no recipe that uses any of them, so until these entries
            // they were inert. Constantan also has TFMG's ingot, and our own fluid for a TFMG pack with
            // neither ATO nor Metallurgy.
            metal("constantan", Heat.HEATED, SHARED, Source.standard(ATO, "constantan"),
                    Source.standard(TFMG, "constantan")),
            metal("invar", Heat.HEATED, SHARED, Source.standard(ATO, "invar")),
            metal("iridium", Heat.HEATED, List.of(ATO), Source.standard(ATO, "iridium")),
            metal("lumium", Heat.HEATED, List.of(ATO), Source.standard(ATO, "lumium")),
            metal("signalum", Heat.HEATED, List.of(ATO), Source.standard(ATO, "signalum")),
            // Metallurgy's own alloys, which only its machines could handle. Superheated because
            // Metallurgy melts all three superheated. It has an obdurium ingot and block, no nugget, and
            // casts necromium and void steel into c:ingots tags no installed mod fills -- so those two
            // are fluid-only here, as kyrmot is.
            metal("obdurium", Heat.SUPERHEATED, List.of(METALLURGY),
                    Source.from(METALLURGY, "createmetallurgy:obdurium_ingot", null,
                            "createmetallurgy:obdurium_block")),
            unsourced("necromium", Heat.SUPERHEATED, List.of(METALLURGY)),
            unsourced("void_steel", Heat.SUPERHEATED, List.of(METALLURGY)),
            metal("lead", Heat.HEATED, SHARED, Source.standard(ATO, "lead"),
                    mekanism("lead"), Source.standard(TFMG, "lead")),
            metal("nickel", Heat.HEATED, SHARED, Source.standard(ATO, "nickel"),
                    Source.standard(TFMG, "nickel")),
            metal("osmium", Heat.HEATED, SHARED, Source.standard(ATO, "osmium"),
                    mekanism("osmium"), Source.standard(MYTHIC_METALS, "osmium")),
            // Mythic Metals' platinum is what star platinum alloys from, in a pack without ATO.
            metal("platinum", Heat.HEATED, List.of(ATO), Source.standard(ATO, "platinum"),
                    Source.standard(MYTHIC_METALS, "platinum")),
            metal("silver", Heat.HEATED, SHARED, Source.standard(ATO, "silver"),
                    Source.standard(MYTHIC_METALS, "silver")),
            // Metallurgy supplies the solid forms, and Northstar where Metallurgy is absent -- which is
            // exactly when our own molten tungsten registers.
            metal("tungsten", Heat.HEATED, List.of(METALLURGY), Source.standard(METALLURGY, "tungsten"),
                    Source.standard(NORTHSTAR, "tungsten")),
            // Our molten uranium registers only with Mekanism and without ATO, so Mekanism's ingot is the
            // only one it can ever cast into.
            metal("uranium", Heat.HEATED, List.of(ATO), Source.standard(ATO, "uranium"),
                    mekanism("uranium")),
            // Metals nothing else ships a fluid for, each gated on something having supplied the metal.
            metal("adamantite", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "adamantite")),
            // BetterEnd's, not Mythic Metals'. This read Source.standard(MYTHIC_METALS, "aeternium")
            // and so cast to mythicmetals:aeternium_ingot, which does not exist in any Mythic Metals
            // version -- the recipe failed to parse whenever that mod loaded. Ingot only: BetterEnd
            // ships no aeternium nugget or block.
            metal("aeternium", Heat.HEATED, List.of(),
                    Source.from(BETTER_END, "betterend:aeternium_ingot", null, null)),
            metal("aquarium", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "aquarium")),
            metal("banglum", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "banglum")),
            metal("carmot", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "carmot")),
            metal("celestium", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "celestium")),
            metal("durasteel", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "durasteel")),
            metal("hallowed", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "hallowed")),
            metal("kyber", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "kyber")),
            metal("manganese", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "manganese")),
            metal("metallurgium", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "metallurgium")),
            metal("midas_gold", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "midas_gold")),
            metal("mythril", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "mythril")),
            metal("orichalcum", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "orichalcum")),
            metal("palladium", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "palladium")),
            metal("prometheum", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "prometheum")),
            metal("quadrillum", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "quadrillum")),
            metal("runite", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "runite")),
            metal("star_platinum", Heat.HEATED, List.of(),
                    Source.from(MYTHIC_METALS, "mythicmetals:star_platinum",
                            "mythicmetals:star_platinum_nugget", "mythicmetals:star_platinum_block")),
            metal("starrite", Heat.HEATED, List.of(),
                    Source.from(MYTHIC_METALS, "mythicmetals:starrite", null, "mythicmetals:starrite_block")),
            metal("stormyx", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "stormyx")),

            // No Source, so casting() skips them; foreignCasts() in the recipe provider names each
            // owner's ingot outright instead, and Metallurgy's table casting also closes the round
            // trip where it is installed. Morkite stays uncast: it is a mineral, not an ingot.
            unsourced("cincinnasite", Heat.HEATED),
            unsourced("morkite", Heat.HEATED),
            unsourced("terminite", Heat.HEATED),
            unsourced("thallasium", Heat.HEATED),

            // Metallurgy owns molten lithium where installed, but has no lithium solids of its own; TFMG's
            // are the ones to cast into. Our fluid registers only without Metallurgy, so in a TFMG pack
            // this was a fluid made from TFMG's ingots with no way back to them.
            metal("lithium", Heat.HEATED, List.of(METALLURGY), Source.standard(TFMG, "lithium")),

            // Create Big Cannons'. Nethersteel is its alone; cast iron it shares with TFMG, whose ingot is
            // the fallback -- our molten cast iron registers precisely when Big Cannons is absent.
            metal("cast_iron", Heat.HEATED, List.of(BIG_CANNONS),
                    Source.from(BIG_CANNONS, "createbigcannons:cast_iron_ingot",
                            "createbigcannons:cast_iron_nugget", "createbigcannons:cast_iron_block"),
                    Source.standard(TFMG, "cast_iron")),
            metal("nethersteel", Heat.HEATED, List.of(BIG_CANNONS),
                    Source.from(BIG_CANNONS, "createbigcannons:nethersteel_ingot",
                            "createbigcannons:nethersteel_nugget", "createbigcannons:nethersteel_block")),

            // Northstar's. Martian steel has no nugget; only ingots are cast.
            metal("titanium", Heat.HEATED, List.of(), Source.standard(NORTHSTAR, "titanium")),
            metal("martian_steel", Heat.HEATED, List.of(),
                    Source.from(NORTHSTAR, "northstar:martian_steel_ingot", null,
                            "northstar:martian_steel_block")),
            // The new alloys from Mythic Metals, and fluid-only on purpose: 0.24.6 has no item,
            // block or translation key for any of the three, so there is nothing to cast into. They
            // are intermediates -- hallowed mixes from mythantite, metallurgium from mythantite and
            // orichadium -- and collapsedAlloys() is the route that skips them for anyone on a
            // version without them.
            unsourced("kyrmot", Heat.HEATED),
            unsourced("mythantite", Heat.HEATED),
            unsourced("orichadium", Heat.HEATED),

            // TFMG's. Magnetic alloy has no nugget, and its laminated block is nine sheets rather than
            // an ingot storage block, so only ingots are cast. TFMG has no molten form of its own.
            metal("magnetic_alloy", Heat.HEATED, List.of(),
                    Source.from(TFMG, "tfmg:magnetic_alloy_ingot", null, null)),
            // TFMG calls this one liquid_silicon rather than molten_silicon, so the owner entry names
            // the fluid outright instead of letting it be derived from the metal.
            metal("silicon", Heat.HEATED, List.of("tfmg:liquid_silicon"),
                    Source.from(TFMG, "tfmg:silicon_ingot", null, null)));

    /** Mekanism spells its solids prefix-first: {@code ingot_tin}, not {@code tin_ingot}. */
    private static Source mekanism(String metal) {
        return Source.from(MEKANISM, "mekanism:ingot_" + metal, "mekanism:nugget_" + metal,
                "mekanism:block_" + metal);
    }

    private static Metal metal(String name, Heat heat, List<String> fluidOwners, Source... sources) {
        return new Metal(name, heat, fluidOwners, List.of(sources), true);
    }

    private static Metal unsourced(String name, Heat heat) {
        return unsourced(name, heat, List.of());
    }

    private static Metal unsourced(String name, Heat heat, List<String> fluidOwners) {
        return new Metal(name, heat, fluidOwners, List.of(), true);
    }

    private static Metal special(String name, Heat heat, Source... sources) {
        return new Metal(name, heat, List.of(), List.of(sources), false);
    }

    private Metals() {}
}
