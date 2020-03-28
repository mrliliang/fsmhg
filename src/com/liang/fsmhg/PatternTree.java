package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.enumerator.Enumerator;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.*;

public class PatternTree {
    private Map<Long, LabeledGraph> trans;
    private Map<Integer, PointPattern> points;

    private Map<DFSEdge, Pattern> edges;

    private Enumerator enumerator;
    private int clusterCounter;

    public PatternTree(Enumerator enumerator) {
        this.enumerator = enumerator;
        trans = new TreeMap<>();
        points = new TreeMap<>();
        edges = new TreeMap<>();
    }

    public void shrink(List<Long> graphIds) {
        TreeSet<LabeledGraph> set = new TreeSet<>();
    }

    public void grow(Map<Long, ? extends LabeledGraph> newTrans, double minSupport, double similarity, int maxEdgeSize) {
        trans.putAll(newTrans);
        List<Cluster> clusters = Cluster.partition(new ArrayList<>(newTrans.values()), similarity, clusterCounter);
        clusterCounter += clusters.size();
        Map<Integer, PointPattern> points = enumerator.points(this.points, clusters);
        Map<DFSEdge, Pattern> edges = enumerator.edges(new ArrayList<>(points.values()), clusters);

        for (Pattern p : edges.values()) {
            if (p.frequency() < minSupport * trans.size()) {
                continue;
            }
            if (p.code().edgeSize() >= maxEdgeSize) {
                return;
            }

            enumerator.subgraphMining(trans, p, minSupport, maxEdgeSize);
        }

    }
}
