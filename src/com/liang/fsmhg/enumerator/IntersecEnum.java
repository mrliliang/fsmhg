package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.graph.LabeledGraph;

import java.util.TreeMap;

public class IntersecEnum extends ClusterEnumerator {


    public IntersecEnum(TreeMap<Long, LabeledGraph> trans, double minSupport, double similarity) {
        super(trans, minSupport, similarity);
    }
}
