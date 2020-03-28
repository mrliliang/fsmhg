package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.*;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Enumerator {
    private Map<Integer, PointPattern> points;
    private int clusterCounter;

    public void enumerate(Map<Long, LabeledGraph> newTrans, double minSupport, double similarity, int maxEdgeSize) {
        List<Cluster> clusters = Cluster.partition(new ArrayList<>(newTrans.values()), similarity, clusterCounter);
        clusterCounter += clusters.size();
        Map<Integer, PointPattern> points = points(this.points, clusters);
        Map<DFSEdge, Pattern> edges = edges(new ArrayList<>(points.values()), clusters);

        for (Pattern p : edges.values()) {
            if (p.frequency() < minSupport || p.code().edgeSize() < maxEdgeSize) {
                continue;
            }
            subgraphMining(newTrans, p, minSupport, maxEdgeSize);
        }
    }


    public Map<Integer, PointPattern> points(Map<Integer, PointPattern> oldPoints, List<Cluster> clusters) {
        Map<Integer, PointPattern> newPoints = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionPoints(c, oldPoints, newPoints);
            otherPoints(c, oldPoints, newPoints);
        }
        return newPoints;
    }

    private void intersectionPoints(Cluster c, Map<Integer, PointPattern> oldPoints, Map<Integer, PointPattern> newPoints) {
        Cluster.Intersection inter = c.intersection();
        Map<Integer, LabeledVertex> border = c.border();
        for (LabeledVertex v : inter.vertices()) {
            PointPattern pattern = oldPoints.get(inter.vLabel(v));
            if (pattern == null) {
                pattern = new PointPattern(inter.vLabel(v));
                oldPoints.put(pattern.label(), pattern);
            }
            newPoints.put(pattern.label(), pattern);
            Embedding em = new Embedding(v, null);
            if (isBorderEmbedding(em, border)) {
                pattern.addBorderEmbedding(c, em);
            } else {
                pattern.addIntersectionEmbedding(c, em);
            }
        }
    }


    private void otherPoints(Cluster c, Map<Integer, PointPattern> oldPoints, Map<Integer, PointPattern> newPoints) {
        for (Cluster.DeltaGraph g : c.deltaGraphs()) {
            Map<Integer, LabeledVertex> border = g.border();
            for (LabeledVertex v : g.vertices()) {
                if (border.containsKey(v.id())) {
                    continue;
                }
                PointPattern pattern = oldPoints.get(g.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(g.vLabel(v));
                    oldPoints.put(pattern.label(), pattern);
                }
                newPoints.put(pattern.label(), pattern);
                Embedding em = new Embedding(v, null);
                pattern.addEmbedding(g, em);
            }
        }
    }

    public Map<DFSEdge, Pattern> edges(List<PointPattern> points, List<Cluster> clusters) {
        Map<DFSEdge, Pattern> newEdges = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionEdges(c, points, newEdges);
            otherEdges(c, points, newEdges);
        }
        return newEdges;
    }

    private void intersectionEdges(Cluster c, List<PointPattern> points, Map<DFSEdge, Pattern> newEdges) {
        Cluster.Intersection inter = c.intersection();
        for (PointPattern p : points) {
            for (Embedding em : p.borderEmbeddings(c)) {
                for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                    if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                        continue;
                    }
                    Pattern child = p.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    if (child == null) {
                        child = p.addChild(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    }
                    child.addBorderEmbedding(c, new Embedding(e.to(), em));
                    newEdges.put(child.edge(), child);
                }
            }

            for (Embedding em : p.intersectionEmbeddings(c)) {
                for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                    if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                        continue;
                    }
                    Pattern child = p.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    if (child == null) {
                        child = p.addChild(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    }
                    child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                    newEdges.put(child.edge(), child);
                }
            }
        }
    }

    private void otherEdges (Cluster c, List<PointPattern> points, Map<DFSEdge, Pattern> newEdges) {
        List<? extends LabeledGraph> deltaGraphs = c.deltaGraphs();
        for (LabeledGraph g : deltaGraphs) {
            for (PointPattern p : points) {
                for (Embedding em : p.embeddings(g.graphId())) {
                    for (LabeledEdge e : g.adjEdges(em.vertex().id())) {
                        if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                            continue;
                        }
                        Pattern child = p.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        if (child == null) {
                            child = p.addChild(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        }
                        child.addEmbedding(g, new Embedding(e.to(), em));
                        newEdges.put(child.edge(), child);
                    }
                }
            }
        }
    }

    private boolean isBorderEmbedding(Embedding em, Map<Integer, LabeledVertex> border) {
        for (LabeledVertex v : em.vertices()) {
            if (border.containsKey(v.id())) {
                return true;
            }
        }
        return false;
    }


    public void subgraphMining(Map<Long, LabeledGraph> trans, Pattern parent, double minSup, int maxEdgeSize) {
        if (!parent.checkMin()) {
            return;
        }

        List<Pattern> children = enumerateChildren(parent);
        if (children == null || children.size() == 0) {
            return;
        }
        for (Pattern p : children) {
            if (p.code().edgeSize() >= maxEdgeSize) {
                return;
            }
            if (p.frequency() < minSup * trans.size()) {
                continue;
            }
            subgraphMining(trans, p, minSup, maxEdgeSize);
        }
    }

    private List<Pattern> enumerateChildren(Pattern p) {
        // TODO: 2020/3/27 enumerate children
        Map<DFSEdge, Pattern> newChildren = new TreeMap<>();
        for (Cluster c : p.newClusters()) {
            expandInterEmbeddings(p, c, newChildren);
            expandBorderEmbeddings(p, c, newChildren);
        }
        for (LabeledGraph g : p.unClusteredGraphs()) {
            expandOtherEmbeddings(p, g, newChildren);
        }
        return new ArrayList<>(newChildren.values());
    }

    private void expandInterEmbeddings(Pattern p, Cluster c, Map<DFSEdge, Pattern> newChildren) {
        LabeledGraph inter = c.intersection();
        List<Integer> rmPath = p.code().rightMostPath();


        for (Embedding em : p.intersectionEmbeddings(c)) {
            for (int v : rmPath) {
                // TODO: 2020/3/28 backward edge

                // TODO: 2020/3/28 forward edges on right most vertex

                // TODO: 2020/3/28 forward edges on right most path
            }
        }

        for (Embedding em : p.borderEmbeddings(c)) {
            for (int v : rmPath) {
                // TODO: 2020/3/28 backward edge

                // TODO: 2020/3/28 forward edges on right most vertex

                // TODO: 2020/3/28 forward edges on right most path
            }
        }

    }

    private void expandBorderEmbeddings(Pattern p, Cluster c, Map<DFSEdge, Pattern> newChildren) {

    }

    private void expandOtherEmbeddings(Pattern p, LabeledGraph g, Map<DFSEdge, Pattern> newChildren) {

    }

    private void getBackwardEdge() {

    }

    private void getRmVertexForward() {

    }

    private void getRmPathForward() {

    }

}
