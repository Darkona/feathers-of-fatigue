package com.darkona.feathersoffatigue.weight;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

public final class WeightSplit {

    final int[] parts = new int[ArmorWeights.PARTS];
    final IntArrayList sources = new IntArrayList();
    final DoubleArrayList shares = new DoubleArrayList();

    public int part(int part) {
        return parts[part];
    }

    public int sourceCount() {
        return sources.size() / 2;
    }

    public int tint(int source) {
        return sources.getInt(2 * source);
    }

    public int feathers(int source) {
        return sources.getInt(2 * source + 1);
    }

    public void clear() {
        Arrays.fill(parts, 0);
        sources.clear();
        shares.clear();
    }

    public int[] toArray() {
        int[] out = new int[ArmorWeights.PARTS + sources.size()];
        System.arraycopy(parts, 0, out, 0, ArmorWeights.PARTS);
        sources.getElements(0, out, ArmorWeights.PARTS, sources.size());
        return out;
    }

    public void readFrom(int[] flat) {
        clear();
        System.arraycopy(flat, 0, parts, 0, ArmorWeights.PARTS);
        sources.addElements(0, flat, ArmorWeights.PARTS, flat.length - ArmorWeights.PARTS);
    }

    public boolean sameAs(WeightSplit other) {
        return Arrays.equals(parts, other.parts) && sources.equals(other.sources);
    }

    public void copyFrom(WeightSplit other) {
        System.arraycopy(other.parts, 0, parts, 0, ArmorWeights.PARTS);
        sources.clear();
        sources.addAll(other.sources);
    }
}
