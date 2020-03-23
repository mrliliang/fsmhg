package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.PatternTree;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.List;

public abstract class Enumerator {

    private Operation op;

    public Enumerator(Operation op) {
        this.op = op;
    }

    public abstract PatternTree enumerate(List<? extends LabeledGraph> graphs, double minSupport, double similarity, int maxEdgeNum);

    public Operation op() {
        return op;
    }

}
