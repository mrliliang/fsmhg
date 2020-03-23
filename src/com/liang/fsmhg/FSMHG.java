package com.liang.fsmhg;

import com.liang.fsmhg.enumerator.Enumerator;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.List;

public class FSMHG {

    private boolean storePatterns;
    private Enumerator enumerator;

    public FSMHG(Enumerator enumerator) {
        this.enumerator = enumerator;
    }

    public PatternTree mine(List<? extends LabeledGraph> graphs, double minSupport, double similarity, int maxEdgeNum) {
        return enumerator.enumerate(graphs, minSupport, similarity, maxEdgeNum);
    }

}
