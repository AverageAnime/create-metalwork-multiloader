package dev.averageanime.createmetalwork.neoforge.datagen;

/**
 * Create: Metalwork's own ingot maths, in millibuckets.
 *
 * <p>Deliberately not in the shared library: another mod measuring the same fluids can land on a
 * different unit, and a library constant would quietly impose this one.
 */
public final class Amounts {

    public static final int UNIT = 90;
    public static final int NUGGET = UNIT / 9;
    public static final int BLOCK = UNIT * 9;

    private Amounts() {}
}
