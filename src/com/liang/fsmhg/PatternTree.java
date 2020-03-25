package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.enumerator.Enumerator;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.*;

public class PatternTree {
    private Map<Integer, PointPattern> points;

    private Map<DFSEdge, Pattern> edges;

    private Enumerator enumerator;

    public PatternTree(Enumerator enumerator) {
        this.enumerator = enumerator;
        points = new TreeMap<>();
        edges = new TreeMap<>();
    }

    public void shrink(List<Long> graphIds) {
        TreeSet<LabeledGraph> set = new TreeSet<>();
    }

    public void grow(Map<Long, ? extends LabeledGraph> newTrans, double minSupport, double similarity, int maxEdgeSize) {
        List<Cluster> clusters = Cluster.partition(new ArrayList<>(newTrans.values()), similarity);
        List<PointPattern> points = enumerator.points(this.points, clusters);

        List<Pattern> edges = enumerator.edges(points, this.edges, clusters);

    }
}
