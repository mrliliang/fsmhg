package com.liang.fsmhg;

import java.util.NavigableMap;
import java.util.TreeMap;

public class DynamicVertex extends Vertex {

    private NavigableMap<Long, Integer> labels;

    public DynamicVertex(int id) {
        super(id);
        labels = new TreeMap<>();
    }

    public int label(long time) {
        return labels.floorEntry(time).getValue();
    }

    public void addLabel(long time, int label) {
        int oldLabel = labels.lastEntry().getValue();
        if (oldLabel != label)
        {
            labels.put(time, label);
        }
    }

}
