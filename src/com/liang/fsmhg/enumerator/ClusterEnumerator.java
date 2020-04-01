package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.*;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

import java.util.*;

public class ClusterEnumerator {
    private TreeMap<Integer, PointPattern> points;
    private TreeMap<Long, LabeledGraph> trans;
    private double minSup;
    private int maxEdgeSize = Integer.MAX_VALUE;

    private double similarity;
    private int clusterCounter;

    private LabeledGraph transDelimiter;
    private Cluster clusterDemimiter;

    public ClusterEnumerator(TreeMap<Long, LabeledGraph> trans, double minSupport, double similarity) {
        this.trans = trans;
        this.minSup = minSupport;
        this.similarity = similarity;
        this.points = new TreeMap<>();
    }

    // TODO: 2020/3/31 enumeration
    public void enumerate() {
        transDelimiter = trans.firstEntry().getValue();
        List<Cluster> clusters = Cluster.partition(new ArrayList<>(trans.values()), similarity, 0);
        clusterDemimiter = clusters.get(0);
        clusterCounter = clusters.size();
        Map<Integer, PointPattern> points = newPoints(clusters);
        Map<DFSEdge, Pattern> edges = newEdges(points, clusters);

        List<Pattern> patterns = new ArrayList<>(edges.values());
        for (int i = 0; i < patterns.size(); i++) {
            Pattern p = patterns.get(i);
            if (!isFrequent(p) || p.code().edgeSize() < maxEdgeSize) {
                continue;
            }
            subgraphMining(trans, p, rightSiblings(p, patterns, i + 1));
        }
    }

    private List<Pattern> rightSiblings(Pattern p, List<Pattern> patterns, int fromIndex) {
        int toIndex = fromIndex;
        while (patterns.get(toIndex).parent() != p.parent()) {
            toIndex++;
        }
        return patterns.subList(fromIndex, toIndex);
    }

    // TODO: 2020/3/31 increment enumeration
    public void incrementEnum(TreeMap<Long, LabeledGraph> newTrans) {
        transDelimiter = newTrans.firstEntry().getValue();
        this.trans.putAll(newTrans);
        List<Cluster> clusters = Cluster.partition(new ArrayList<>(newTrans.values()), similarity, clusterCounter);
        clusterDemimiter = clusters.get(0);
        clusterCounter += clusters.size();
        Map<Integer, PointPattern> points = newPoints(clusters);
        Map<DFSEdge, Pattern> edges = newEdges(points, clusters);

        List<Pattern> patterns = new ArrayList<>(edges.values());
        for (int i = 0; i < patterns.size(); i++) {
            Pattern p = patterns.get(i);
            if (!isFrequent(p) || p.code().edgeSize() < maxEdgeSize) {
                continue;
            }
            subgraphMining(newTrans, p, rightSiblings(p, patterns, i + 1));
        }
    }


    public Map<Integer, PointPattern> newPoints(List<Cluster> clusters) {
        Map<Integer, PointPattern> newPoints = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionPoints(c, newPoints);
            otherPoints(c, newPoints);
        }
        return newPoints;
    }

    private void intersectionPoints(Cluster c, Map<Integer, PointPattern> newPoints) {
        Cluster.Intersection inter = c.intersection();
        Map<Integer, LabeledVertex> border = c.border();
        for (LabeledVertex v : inter.vertices()) {
            PointPattern pattern = this.points.get(inter.vLabel(v));
            if (pattern == null) {
                pattern = new PointPattern(inter.vLabel(v));
                this.points.put(pattern.label(), pattern);
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


    private void otherPoints(Cluster c, Map<Integer, PointPattern> newPoints) {
        for (Cluster.DeltaGraph g : c.deltaGraphs()) {
            Map<Integer, LabeledVertex> border = g.border();
            for (LabeledVertex v : g.vertices()) {
                if (border.containsKey(v.id())) {
                    continue;
                }
                PointPattern pattern = this.points.get(g.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(g.vLabel(v));
                    this.points.put(pattern.label(), pattern);
                }
                newPoints.put(pattern.label(), pattern);
                Embedding em = new Embedding(v, null);
                pattern.addEmbedding(g, em);
            }
        }
    }

    public Map<DFSEdge, Pattern> newEdges(Map<Integer, PointPattern> newPoints, List<Cluster> clusters) {
        Map<DFSEdge, Pattern> newEdges = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionEdges(c, newPoints, newEdges);
            otherEdges(c, newPoints, newEdges);
        }
        return newEdges;
    }

    private void intersectionEdges(Cluster c, Map<Integer, PointPattern> newPoints, Map<DFSEdge, Pattern> newEdges) {
        Cluster.Intersection inter = c.intersection();

        for (PointPattern p : newPoints.values()) {
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

    private void otherEdges (Cluster c, Map<Integer, PointPattern> newPoints, Map<DFSEdge, Pattern> newEdges) {
        for (LabeledGraph g : c) {
            Cluster.DeltaGraph delta = c.deltaGraph(g);
            for (PointPattern p : newPoints.values()) {
                for (Embedding em : p.embeddings(g)) {
                    for (LabeledEdge e : delta.adjEdges(em.vertex().id())) {
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

                for (Embedding em : p.borderEmbeddings(c)) {
                    if (!delta.border().containsKey(em.vertex().id())) {
                        continue;
                    }
                    for (LabeledEdge e : delta.adjEdges(em.vertex().id())) {
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


    public void subgraphMining(Map<Long, LabeledGraph> trans, Pattern parent, List<Pattern> siblings) {
        if (!parent.checkMin()) {
            return;
        }

        List<Pattern> children = enumerateChildren(parent, siblings);
        if (children == null || children.size() == 0) {
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            Pattern p = children.get(i);
            if (p.code().edgeSize() >= maxEdgeSize) {
                return;
            }
            if (!isFrequent(p)) {
                continue;
            }
            subgraphMining(trans, p, children.subList(i + 1, children.size()));
        }
    }

    private List<Pattern> enumerateChildren(Pattern p, List<Pattern> siblings) {
        Map<DFSEdge, Pattern> newChildren = new TreeMap<>();
        for (Pattern sibling : siblings) {
            if (!isFrequent(sibling)) {
                continue;
            }
            Pattern child = join(p, sibling);
            newChildren.put(child.edge(), child);
        }

        newChildren.putAll(extend(p));

//        for (Cluster c : p.clusters(clusterDemimiter)) {
//            expandInterEmbeddings(p, c, newChildren);
//            expandBorderEmbeddings(p, c, newChildren);
//        }
//        for (LabeledGraph g : p.unClusteredGraphs(graphDelimiter)) {
//            expandOtherEmbeddings(p, g, newChildren);
//        }
//        return new ArrayList<>(newChildren.values());
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
                    Pattern child = updateInterExpansion(c, rmPath.get(rmPath.size() - 1), rmPath.get(i), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                    newChildren.put(child.edge(), child);
                }

                // TODO: 2020/3/28 forward edges on right most paht vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(inter, emVertices, emBit, i);
                for (LabeledEdge e : forwardEdges) {
                    Pattern child = updateInterExpansion(c, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), em, p);
                    newChildren.put(child.edge(), child);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(inter, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                Pattern child = updateInterExpansion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), em, p);
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
                    Pattern child = updateBorderExpansion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                    newChildren.put(child.edge(), child);
                }

                // TODO: 2020/3/28 forward edges on right most path vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(inter, emVertices, emBit, rmPath.get(i));
                for (LabeledEdge e : forwardEdges) {
                    Pattern child = updateBorderExpansion(c, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), em, p);
                    newChildren.put(child.edge(), child);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(inter, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                Pattern child = updateBorderExpansion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), em, p);
                newChildren.put(child.edge(), child);
            }


            for (LabeledGraph g : c) {
                for (int i = 0; i < rmPath.size() - 1; i++) {
                    LabeledEdge backEdge = getDeltaBackwardEdge(g, pg, c.deltaGraph(g), emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                    if (backEdge == null) {
                        Pattern child = updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                        newChildren.put(child.edge(), child);
                    }

                    List<LabeledEdge> forwardEdges = getDeltaRmPathForward(g, c.deltaGraph(g), emVertices, emBit, rmPath.get(i));
                    for (LabeledEdge e : forwardEdges) {
                        Pattern child = updateOtherExpansion(g, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), em, p);
                        newChildren.put(child.edge(), child);
                    }
                }

                rmForwardEdges = getDeltaRmVertexForward(g, c.deltaGraph(g), emVertices, emBit, rmPath.get(rmPath.size() - 1));
                for (LabeledEdge e : rmForwardEdges) {
                    Pattern child = updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), em, p);
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
                    updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), rmPath.get(i), g.vLabel(backEdge.to()), g.vLabel(backEdge.from()), g.eLabel(backEdge), em, p);
                }

                // TODO: 2020/3/28 forward edges on right most path vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(g, emVertices, emBit, i);
                for (LabeledEdge e : forwardEdges) {
                    updateOtherExpansion(g, rmPath.get(i), emVertices.size(), g.vLabel(e.from()), g.vLabel(backEdge.to()), g.eLabel(e), em, p);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(g, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e), em, p);
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


    private Pattern updateInterExpansion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addIntersectionEmbedding(c, em);
        return child;
    }

    private Pattern updateBorderExpansion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addBorderEmbedding(c, em);
        return child;
    }

    private Pattern updateOtherExpansion(LabeledGraph g, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addEmbedding(g, em);
        return child;
    }

    private void join(Pattern p, List<Pattern> siblings, TreeMap<DFSEdge, Pattern> children) {
        // TODO: 2020/3/31 join
        TreeMap<Integer, TreeSet<DFSEdge>> backCand = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> forCand = new TreeMap<>();

        DFSEdge e1 = p.edge();
        for (Pattern sib : siblings) {
            DFSEdge e2 = sib.edge();
            if (e1.compareTo(e2) >= 0) {
                continue;
            }

            if (!e1.isForward() && !e2.isForward() && e1.to() == e2.to()) {
                continue;
            }

            TreeSet<DFSEdge> candidates = backCand.computeIfAbsent(e2.to(), vIndex -> new TreeSet<>());
            if (!e2.isForward()) {
                candidates.add(e2);
            } else {
                candidates = forCand.computeIfAbsent(e2.to(), vIndex -> new TreeSet<>());
                candidates.add(new DFSEdge(e2.from(), p.code().nodeCount(), e2.fromLabel(), e2.toLabel(), e2.edgeLabel()));
            }
        }

        TreeSet<Cluster> commonCluster = new TreeSet<>();
        TreeSet<LabeledGraph> commonTrans = new TreeSet<>();
        for (Pattern sib : siblings) {
            commonCluster.addAll(sib.clusters(clusterDemimiter));
            commonTrans.addAll(sib.unClusteredGraphs(transDelimiter));
        }
        commonCluster.retainAll(p.clusters(clusterDemimiter));
        commonTrans.retainAll(p.unClusteredGraphs(transDelimiter));

        for (Cluster c : commonCluster) {
            joinInterEmbeddings(c, p, backCand, forCand, children);
            joinBorderEmbeddings(c, p, backCand, forCand, children);
        }
        for (LabeledGraph g : commonTrans) {
            joinOtherEmbeddings(g, p, backCand, forCand, children);
        }
    }

    private TreeMap<DFSEdge, Pattern> extend(Pattern p) {
        TreeMap<DFSEdge, Pattern> children = new TreeMap<>();

        return children;
    }

    private void joinInterEmbeddings(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeMap<DFSEdge, Pattern> children) {
        LabeledGraph inter = c.intersection();
        for (Embedding em : p.intersectionEmbeddings(c)) {
            List<LabeledVertex> emVertics = em.vertices();
            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertics.get(emVertics.size() - 1);
                LabeledVertex to = emVertics.get(entry.getKey());
                LabeledEdge back = inter.edge(from.id(), to.id());
                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(emVertics.size() - 1, entry.getKey(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                if (cands.ceiling(dfsEdge) != null) {
                    Pattern child = updateInterExpansion(c, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), em, p);
                    children.put(child.edge(), child);
                }
            }

            //join forward edge
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertics.get(entry.getKey());
                for (LabeledEdge e : inter.adjEdges(from.id())) {

                }
            }
        }
    }

    private void joinBorderEmbeddings(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeMap<DFSEdge, Pattern> children) {

    }

    private void joinOtherEmbeddings(LabeledGraph g, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeMap<DFSEdge, Pattern> children)


    private boolean isFrequent(Pattern p) {
        return p.frequency() >= this.trans.size() * minSup;
    }

}
