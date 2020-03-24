package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.Cluster;
import com.liang.fsmhg.PointPattern;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.List;

public class Enumerator {

    List<PointPattern> points(List<? extends LabeledGraph> trans) {
        List<Cluster> clusters = Cluster.partition(trans, 1);
        return null;
    }

    List<DFSEdge> edges(List<LabeledGraph> trans) {
        return null;
    }

}
