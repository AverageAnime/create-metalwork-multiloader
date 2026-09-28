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

            // Shared with Create: Ironworks, which wins when present; ATO is the fallback.
            metal("tin", Heat.HEATED, SHARED,
                    Source.from(IRONWORKS, "create_ironworks:tin_ingot", null, "create_ironworks:tin_block"),
                    Source.standard(ATO, "tin")),
            // TFMG and Big Cannons ship c:molten_steel too. TFMG values an ingot at 144; the five recipes
            // it has for the fluid are restated at 90 in its own namespace by tfmgSteel.
            metal("steel", Heat.HEATED, List.of(METALLURGY, ATO, TFMG, BIG_CANNONS),
                    Source.from(IRONWORKS, "create_ironworks:steel_ingot", null, "create_ironworks:steel_block"),
                    Source.standard(ATO, "steel")),
            metal("bronze", Heat.HEATED, List.of(METALLURGY, ATO, BIG_CANNONS),
                    Source.from(IRONWORKS, "create_ironworks:bronze_ingot", null, "create_ironworks:bronze_block"),
                    Source.standard(ATO, "bronze")),

            metal("aluminum", Heat.HEATED, SHARED, Source.standard(ATO, "aluminum")),
            metal("electrum", Heat.HEATED, SHARED, Source.standard(ATO, "electrum")),
            metal("enderium", Heat.HEATED, List.of(ATO), Source.standard(ATO, "enderium")),
            metal("lead", Heat.HEATED, SHARED, Source.standard(ATO, "lead")),
            metal("nickel", Heat.HEATED, SHARED, Source.standard(ATO, "nickel")),
            metal("osmium", Heat.HEATED, SHARED, Source.standard(ATO, "osmium")),
            metal("platinum", Heat.HEATED, List.of(ATO), Source.standard(ATO, "platinum")),
            metal("silver", Heat.HEATED, SHARED, Source.standard(ATO, "silver")),
            // Metallurgy supplies the solid forms.
            metal("tungsten", Heat.HEATED, List.of(METALLURGY), Source.standard(METALLURGY, "tungsten")),
            metal("uranium", Heat.HEATED, List.of(ATO), Source.standard(ATO, "uranium")),
            // Metals nothing else ships a fluid for, each gated on something having supplied the metal.
            metal("adamantite", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "adamantite")),
            metal("aeternium", Heat.HEATED, List.of(), Source.standard(MYTHIC_METALS, "aeternium")),
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

            // Melt but do not cast back here; Metallurgy's table casting closes the round trip.
            unsourced("cincinnasite", Heat.HEATED),
            unsourced("morkite", Heat.HEATED),
            unsourced("terminite", Heat.HEATED),
            unsourced("thallasium", Heat.HEATED),

            // Metallurgy owns molten lithium outright and ships the melting and casting recipes for it;
            // it has no lithium solids of its own, so there is nothing here to cast back into.
            unsourced("lithium", Heat.HEATED, List.of(METALLURGY)),

            // Create Big Cannons'. Nethersteel is its alone; cast iron it shares with TFMG.
            metal("cast_iron", Heat.HEATED, List.of(BIG_CANNONS),
                    Source.from(BIG_CANNONS, "createbigcannons:cast_iron_ingot",
                            "createbigcannons:cast_iron_nugget", "createbigcannons:cast_iron_block")),
            metal("nethersteel", Heat.HEATED, List.of(BIG_CANNONS),
                    Source.from(BIG_CANNONS, "createbigcannons:nethersteel_ingot",
                            "createbigcannons:nethersteel_nugget", "createbigcannons:nethersteel_block")),

            // Northstar's. Martian steel has no nugget; only ingots are cast.
            metal("titanium", Heat.HEATED, List.of(), Source.standard(NORTHSTAR, "titanium")),
            metal("martian_steel", Heat.HEATED, List.of(),
                    Source.from(NORTHSTAR, "northstar:martian_steel_ingot", null,
                            "northstar:martian_steel_block")),
            // The new alloys from Mythic Metals.
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
