package com.liang.fsmhg.graph;

import java.util.NavigableMap;
import java.util.TreeMap;

public class DynamicVertex extends LabeledVertex {

    private NavigableMap<Long, Integer> labels;

    protected DynamicVertex(int id) {
        super(id);
        labels = new TreeMap<>();
    }

    public int label(long time) {
        return labels.floorEntry(time).getValue();
    }

    protected void addLabel(long time, int label) {
        int oldLabel = labels.lastEntry().getValue();
        if (oldLabel != label)
        {
            labels.put(time, label);
        }
    }
}
