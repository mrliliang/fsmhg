package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.*;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

import java.util.*;

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
                for (Embedding em : p.embeddings(g)) {
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
        LabeledGraph pg = p.code().toGraph();

        for (Embedding em : p.intersectionEmbeddings(c)) {
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBit = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBit.set(v.id());
            }
            for (int i = 0; i < rmPath.size() - 1; i++) {
                // TODO: 2020/3/28 backward edge, use join operation
                LabeledEdge backEdge = getBackwardEdge(inter, pg, emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                if (backEdge == null) {
                    Pattern child = updateInterExpanssion(c, rmPath.get(rmPath.size() - 1), rmPath.get(i), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                    newChildren.put(child.edge(), child);
                }

                // TODO: 2020/3/28 forward edges on right most paht vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(inter, emVertices, emBit, i);
                for (LabeledEdge e : forwardEdges) {
                    Pattern child = updateInterExpanssion(c, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), em, p);
                    newChildren.put(child.edge(), child);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(inter, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                Pattern child = updateInterExpanssion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), em, p);
                newChildren.put(child.edge(), child);
            }
        }
    }

    private void expandBorderEmbeddings(Pattern p, Cluster c, Map<DFSEdge, Pattern> newChildren) {
        LabeledGraph inter = c.intersection();
        List<Integer> rmPath = p.code().rightMostPath();
        LabeledGraph pg = p.code().toGraph();

        for (Embedding em : p.borderEmbeddings(c)) {
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBit = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBit.set(v.id());
            }
            for (int i = 0; i < rmPath.size() - 1; i++) {
                // TODO: 2020/3/28 backward edge, use join operation
                LabeledEdge backEdge = getBackwardEdge(inter, pg, emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                if (backEdge == null) {
                    Pattern child = updateBorderExpanssion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                    newChildren.put(child.edge(), child);
                }

                // TODO: 2020/3/28 forward edges on right most path vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(inter, emVertices, emBit, rmPath.get(i));
                for (LabeledEdge e : forwardEdges) {
                    Pattern child = updateBorderExpanssion(c, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), em, p);
                    newChildren.put(child.edge(), child);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(inter, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                Pattern child = updateBorderExpanssion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), em, p);
                newChildren.put(child.edge(), child);
            }


            for (LabeledGraph g : c) {
                for (int i = 0; i < rmPath.size() - 1; i++) {
                    LabeledEdge backEdge = getDeltaBackwardEdge(g, pg, c.deltaGraph(g), emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                    if (backEdge == null) {
                        Pattern child = updateOtherExpanssion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                        newChildren.put(child.edge(), child);
                    }

                    List<LabeledEdge> forwardEdges = getDeltaRmPathForward(g, c.deltaGraph(g), emVertices, emBit, rmPath.get(i));
                    for (LabeledEdge e : forwardEdges) {
                        Pattern child = updateOtherExpanssion(g, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), em, p);
                        newChildren.put(child.edge(), child);
                    }
                }

                rmForwardEdges = getDeltaRmVertexForward(g, c.deltaGraph(g), emVertices, emBit, rmPath.get(rmPath.size() - 1));
                for (LabeledEdge e : rmForwardEdges) {
                    Pattern child = updateOtherExpanssion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), em, p);
                    newChildren.put(child.edge(), child);
                }
            }
        }
    }

    private void expandOtherEmbeddings(Pattern p, LabeledGraph g, Map<DFSEdge, Pattern> newChildren) {
        List<Integer> rmPath = p.code().rightMostPath();
        LabeledGraph pg = p.code().toGraph();

        for (Embedding em : p.embeddings(g)) {
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBit = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBit.set(v.id());
            }
            for (int i = 0; i < rmPath.size() - 1; i++) {
                // TODO: 2020/3/28 backward edge, use join operation
                LabeledEdge backEdge = getBackwardEdge(g, pg, emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                if (backEdge == null) {
                    updateOtherExpanssion(g, rmPath.get(rmPath.size() - 1), rmPath.get(i), g.vLabel(backEdge.to()), g.vLabel(backEdge.from()), g.eLabel(backEdge), em, p);
                }

                // TODO: 2020/3/28 forward edges on right most path vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(g, emVertices, emBit, i);
                for (LabeledEdge e : forwardEdges) {
                    updateOtherExpanssion(g, rmPath.get(i), emVertices.size(), g.vLabel(e.from()), g.vLabel(backEdge.to()), g.eLabel(e), em, p);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(g, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                updateOtherExpanssion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e), em, p);
            }
        }
    }

    private LabeledEdge getBackwardEdge(LabeledGraph g, LabeledGraph pg, List<LabeledVertex> vertices, int pathVertexIndex, int rmVertexIndex) {
        if (pg.edge(rmVertexIndex, pathVertexIndex) != null) {
            return null;
        }
        LabeledEdge pathEdge = g.edge(vertices.get(pathVertexIndex).id(), vertices.get(pathVertexIndex + 1).id());
        LabeledEdge backEdge = g.edge(vertices.get(rmVertexIndex).id(), vertices.get(pathVertexIndex).id());
        if (backEdge == null) {
            return null;
        }
        if (g.eLabel(pathEdge) < g.eLabel(backEdge)
                || (g.eLabel(pathEdge) == g.eLabel(backEdge) && g.vLabel(pathEdge.to()) <= g.vLabel(backEdge.from()))) {
            return backEdge;
        }
        return null;
    }

    private List<LabeledEdge> getRmVertexForward(LabeledGraph g, List<LabeledVertex> vertices, BitSet emBit, int rmVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge firstEdge = g.edge(vertices.get(0).id(), vertices.get(1).id());
        for (LabeledEdge e : g.adjEdges(vertices.get(rmVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.vLabel(firstEdge.from()) < g.vLabel(e.to())
                    || (g.vLabel(firstEdge.from()) == g.vLabel(e.to()) && g.eLabel(firstEdge) <= g.eLabel(e))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }

    private List<LabeledEdge> getRmPathForward(LabeledGraph g, List<LabeledVertex> vertices, BitSet emBit, int pathVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge pathEdge = g.edge(vertices.get(pathVertexIndex).id(), vertices.get(pathVertexIndex + 1).id());
        for (LabeledEdge e : g.adjEdges(vertices.get(pathVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.eLabel(pathEdge) < g.eLabel(e)
                    || (g.eLabel(pathEdge) == g.eLabel(e) && g.vLabel(pathEdge.to()) <= g.vLabel(e.to()))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }


    private LabeledEdge getDeltaBackwardEdge(LabeledGraph g, LabeledGraph pg, LabeledGraph delta, List<LabeledVertex> emVertices, int pathVertexIndex, int rmVertexIndex) {
        if (pg.edge(rmVertexIndex, pathVertexIndex) != null) {
            return null;
        }
        LabeledEdge pathEdge = g.edge(emVertices.get(pathVertexIndex).id(), emVertices.get(pathVertexIndex + 1).id());
        LabeledEdge backEdge = delta.edge(emVertices.get(rmVertexIndex).id(), emVertices.get(pathVertexIndex).id());
        if (backEdge == null) {
            return null;
        }

        if (g.eLabel(pathEdge) < g.eLabel(backEdge)
                || (g.eLabel(pathEdge) == g.eLabel(backEdge) && g.vLabel(pathEdge.to()) <= g.vLabel(backEdge.from()))) {
            return backEdge;
        }
        return null;
    }

    private List<LabeledEdge> getDeltaRmVertexForward(LabeledGraph g, LabeledGraph delta, List<LabeledVertex> vertices, BitSet emBit, int rmVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge firstEdge = g.edge(vertices.get(0).id(), vertices.get(1).id());
        for (LabeledEdge e : delta.adjEdges(vertices.get(rmVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.vLabel(firstEdge.from()) < g.vLabel(e.to())
                    || (g.vLabel(firstEdge.from()) == g.vLabel(e.to()) && g.eLabel(firstEdge) <= g.eLabel(e))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }

    private List<LabeledEdge> getDeltaRmPathForward(LabeledGraph g, LabeledGraph delta, List<LabeledVertex> vertices, BitSet emBit, int pathVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge pathEdge = g.edge(vertices.get(pathVertexIndex).id(), vertices.get(pathVertexIndex + 1).id());
        for (LabeledEdge e : delta.adjEdges(vertices.get(pathVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.eLabel(pathEdge) < g.eLabel(e)
                    || (g.eLabel(pathEdge) == g.eLabel(e) && g.vLabel(pathEdge.to()) <= g.vLabel(e.to()))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }


    private Pattern updateInterExpanssion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addIntersectionEmbedding(c, em);
        return child;
    }

    private Pattern updateBorderExpanssion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addBorderEmbedding(c, em);
        return child;
    }

    private Pattern updateOtherExpanssion(LabeledGraph g, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addEmbedding(g, em);
        return child;
    }

}
