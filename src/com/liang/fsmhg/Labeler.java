package com.liang.fsmhg;

import java.util.HashMap;
import java.util.NavigableMap;

public class Labeler {
    private HashMap<Integer, NavigableMap<Long, Integer>> vLabels;
    private HashMap<Integer, HashMap<Integer, NavigableMap<Long, Integer>>> eLabels;

    int vlabel(int vId, long time) {
        return vLabels.get(vId).floorEntry(time).getValue();
    }

    int elabel(int from, int to, long time) {
        return eLabels.get(from).get(to).floorEntry(time).getValue();
    }
}
