package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.Snapshot;

import java.util.ArrayList;
import java.util.List;

public class FSMHG {

    private boolean storePatterns;

    public PatternTree mine(List<? extends LabeledGraph> graphs, double minSupport, double similarity, int maxEdgeNum) {
        List edgePatterns = singleEdgePatterns(graphs);
        for (Object o : edgePatterns) {

        }
        return null;
    }

    private List singleEdgePatterns(List<? extends LabeledGraph> graphs) {
        return null;
    }

}
