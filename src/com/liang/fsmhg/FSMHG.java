package com.liang.fsmhg;

import com.liang.fsmhg.enumerator.ClusterEnumerator;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.Map;

public class FSMHG {

    private ClusterEnumerator enumerator;

    public FSMHG(ClusterEnumerator enumerator) {
        this.enumerator = enumerator;
    }

    public PatternTree mine(Map<Long, ? extends LabeledGraph> graphs, double minSupport, double similarity, int maxEdgeNum) {
        PatternTree tree = new PatternTree(enumerator);
        tree.grow(graphs, minSupport, similarity, maxEdgeNum);
        return tree;
    }

}
