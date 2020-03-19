package com.liang.fsmhg.graph;

import java.util.NavigableMap;
import java.util.TreeMap;

public class DynamicEdge extends AEdge<DynamicVertex> {

    private NavigableMap<Long, Integer> eLabels;

    public DynamicEdge(DynamicVertex from, DynamicVertex to) {
        super(from, to);
        this.eLabels = new TreeMap<>();
    }

    public int label(long time) {
        return eLabels.floorEntry(time).getValue();
    }

    public void addLabel(long time, int label) {
        int oldLabel = eLabels.lastEntry().getValue();
        if (oldLabel != label)
        {
            eLabels.put(time, label);
        }
    }

}
