package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.graph.LabeledGraph;

import java.util.TreeMap;

public class NonIntersecEnum extends ClusterEnumerator {

    public NonIntersecEnum(TreeMap<Long, LabeledGraph> trans, double minSupport, double similarity) {
        super(trans, minSupport, similarity);
    }
}
