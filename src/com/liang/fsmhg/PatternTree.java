package com.liang.fsmhg;

import com.liang.fsmhg.enumerator.Enumerator;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.List;
import java.util.Map;

public class PatternTree {

    private Enumerator enumerator;

    public PatternTree(Enumerator enumerator) {
        this.enumerator = enumerator;
    }

    public void shrink(List<Long> graphIds) {

    }

    public void grow(Map<Long, ? extends LabeledGraph> newTrans, double minSupport, double similarity, int maxEdgeSize) {

    }
}
