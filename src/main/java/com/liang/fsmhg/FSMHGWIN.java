package com.liang.fsmhg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

public class FSMHGWIN {
    private TreeMap<Integer, PointPattern> points;
    private List<LabeledGraph> trans;
    private File out;
    private double minSup;
    private double absSup;
    private int maxEdgeSize = Integer.MAX_VALUE;
    private boolean partition;
    private double similarity;
    private boolean optimize = true;
    private ArrayList<Cluster> clusters;
    private int clusterCounter;
    private int maxVid = 0;
    private LabeledGraph transDelimiter;
    private Cluster clusterDelimiter;
    private int patternCount = 0;
    private int pointCount = 0;


    public FSMHGWIN(double minSupport, int maxEdgeSize, boolean partition, double similarity) {
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.points = new TreeMap<>();
        this.clusters = new ArrayList<>();
        this.trans = new ArrayList<>();
    }

    public void optimize(boolean opt) {
        this.optimize = opt;
    }

    public void enumerate(List<LabeledGraph> newTrans) {
        long startTime = System.currentTimeMillis();
        this.patternCount = 0;
        this.pointCount = 0;

        List<LabeledGraph> removed;
        List<LabeledGraph> added;
        int index = this.trans.indexOf(newTrans.get(0));
        if (index == -1) {
            removed = this.trans;
            this.trans = newTrans;
            added = this.trans;
        } else {
            removed = this.trans.subList(0, index);
            this.trans = newTrans;
            added = this.trans.subList(this.trans.size() - index, this.trans.size());
        }

        System.out.println("Total trans : " + this.trans.size());
        this.absSup = Math.ceil(this.trans.size() * this.minSup);

        shrink(removed);
        grow(added);

        saveResult();
        long endTime = System.currentTimeMillis();
        System.out.println(pointCount + " point patterns");
        System.out.println((this.patternCount - pointCount) + " connected patterns.");
        System.out.println("Duration = " + (endTime - startTime));
    }

    private void shrink(List<LabeledGraph> graphs) {
        List<Cluster> changed = new ArrayList<>();
        Iterator<Cluster> it = this.clusters.iterator();
        while (it.hasNext()) {
            Cluster c = it.next();
            if (!c.remove(graphs)) {
                break;
            }
            if (c.size() == 0) {
                changed.add(c);
            }
        }

        for (PointPattern pp : this.points.values()) {
            pp.remove(graphs, changed);
        }
    }

    private void grow(List<LabeledGraph> added) {
        transDelimiter = added.get(added.size() - 1);
        List<Cluster> addedClusters;
        Map<Integer, PointPattern> addedPoints;
        Map<DFSEdge, Pattern> addedEdges;
        if (partition) {
            addedClusters = Cluster.partition(added, similarity, clusterCounter);
            clusterDelimiter = addedClusters.get(addedClusters.size() - 1);
            clusterCounter += addedClusters.size();
            this.clusters.addAll(addedClusters);
            addedPoints = pointsCluster(addedClusters);
            addedEdges = edgesCluster(addedPoints, addedClusters);
        } else {
            addedPoints = points(added);
            addedEdges = edges(addedPoints, added);
        }

        List<Pattern> patterns = new ArrayList<>(addedEdges.values());
        for (int i = 0; i < patterns.size(); i++) {
            Pattern p = patterns.get(i);
            if (!isFrequent(p)) {
                continue;
            }
            subgraphMining(added, p);
            p.setClusterDelimiter(this.clusterDelimiter);
            p.setGraphDelimiter(this.transDelimiter);
        }
    }


    public TreeMap<Integer, PointPattern> pointsCluster(List<Cluster> clusters) {
        TreeMap<Integer, PointPattern> addedPoints = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionPoints(c, addedPoints);
            otherPoints(c, addedPoints);
        }
        return addedPoints;
    }

    private void intersectionPoints(Cluster c, Map<Integer, PointPattern> addedPoints) {
        LabeledGraph inter = c.intersection();
        Map<Integer, LabeledVertex> border = c.border();
        for (LabeledVertex v : inter.vertices()) {
            PointPattern pattern = this.points.get(inter.vLabel(v));
            if (pattern == null) {
                pattern = new PointPattern(inter.vLabel(v));
                this.points.put(pattern.label(), pattern);
            }
            Embedding em = new Embedding(v, null);
            if (isBorderEmbedding(em, border)) {
                pattern.addBorderEmbedding(c, em);
            } else {
                pattern.addIntersectionEmbedding(c, em);
            }
            addedPoints.put(pattern.label(), pattern);
            if (v.id() > maxVid) {
                maxVid = v.id();
            }
        }
    }

    private void otherPoints(Cluster c, Map<Integer, PointPattern> addedPoints) {
        for (LabeledGraph g : c.snapshots()) {
            Cluster.DeltaGraph dg = c.deltaGraph(g);
            Map<Integer, LabeledVertex> border = dg.border();
            for (LabeledVertex v : g.vertices()) {
                if (border.containsKey(v.id())) {
                    continue;
                }
                PointPattern pattern = this.points.get(g.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(g.vLabel(v));
                    this.points.put(pattern.label(), pattern);
                }
                Embedding em = new Embedding(v, null);
                pattern.addEmbedding(g, em);
                addedPoints.put(pattern.label(), pattern);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }
        }
    }

    public Map<DFSEdge, Pattern> edgesCluster(Map<Integer, PointPattern> addedPoints, List<Cluster> clusters) {
        Map<DFSEdge, Pattern> addedEdges = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionEdges(c, addedPoints, addedEdges);
            otherEdges(c, addedPoints, addedEdges);
        }
        return addedEdges;
    }

    private void intersectionEdges(Cluster c, Map<Integer, PointPattern> addedPoints, Map<DFSEdge, Pattern> addedEdges) {
        LabeledGraph inter = c.intersection();

        for (PointPattern p : addedPoints.values()) {
            for (Embedding em : p.borderEmbeddings(c)) {
                for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                    if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                        continue;
                    }
                    Pattern child = p.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    child.addBorderEmbedding(c, new Embedding(e.to(), em));
                    addedEdges.put(child.edge(), child);
                }
            }

            for (Embedding em : p.intersectionEmbeddings(c)) {
                for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                    if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                        continue;
                    }
                    Pattern child = p.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                    addedEdges.put(child.edge(), child);
                }
            }
        }
    }

    private void otherEdges (Cluster c, Map<Integer, PointPattern> addedPoints, Map<DFSEdge, Pattern> addedEdges) {
        for (LabeledGraph g : c) {
            Cluster.DeltaGraph delta = c.deltaGraph(g);
            for (PointPattern p : addedPoints.values()) {
                for (Embedding em : p.embeddings(g)) {
                    for (LabeledEdge e : delta.adjEdges(em.vertex().id())) {
                        if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                            continue;
                        }
                        Pattern child = p.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        child.addEmbedding(g, new Embedding(e.to(), em));
                        addedEdges.put(child.edge(), child);
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
                        child.addEmbedding(g, new Embedding(e.to(), em));
                        addedEdges.put(child.edge(), child);
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

    public TreeMap<Integer, PointPattern> points(List<LabeledGraph> addedTrans) {
        TreeMap<Integer, PointPattern> addedPoints = new TreeMap<>();
        for (LabeledGraph g : addedTrans) {
            for (LabeledVertex v : g.vertices()) {
                PointPattern pp = this.points.get(g.vLabel(v));
                if (pp == null) {
                    pp = new PointPattern(g.vLabel(v));
                    this.points.put(pp.label(), pp);
                }
                pp.addEmbedding(g, new Embedding(v, null));
                addedPoints.put(pp.label(), pp);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }
        }
        return addedPoints;
    }

    public Map<DFSEdge, Pattern> edges(Map<Integer, PointPattern> addedPoints, List<LabeledGraph> addedTrans) {
        TreeMap<DFSEdge, Pattern> addedEdges = new TreeMap<>();
        for (PointPattern pp : addedPoints.values()) {
            for (LabeledGraph g : addedTrans) {
                for (Embedding em : pp.embeddings(g)) {
                    LabeledVertex from = em.vertex();
                    for (LabeledEdge e : g.adjEdges(from.id())) {
                        LabeledVertex to = e.to();
                        if (g.vLabel(from) > g.vLabel(to)) {
                            continue;
                        }
                        Pattern child = pp.child(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                        child.addEmbedding(g, new Embedding(to, em));
                        addedEdges.put(child.edge(), child);
                    }
                }
            }
        }
        return addedEdges;
    }


    public void subgraphMining(List<LabeledGraph> trans, Pattern parent) {
        if (!parent.checkMin()) {
            return;
        }
        if (parent.code().edgeSize() >= maxEdgeSize) {
            return;
        }

        List<Pattern> children = enumerateChildren(parent);
        for (Pattern child : children) {
            if (!isFrequent(child)) {
                continue;
            }
            subgraphMining(trans, child);
            child.setClusterDelimiter(this.clusterDelimiter);
            child.setGraphDelimiter(this.transDelimiter);
        }
    }

    private List<Pattern> enumerateChildren(Pattern p) {
        TreeMap<DFSEdge, Pattern> addedChildren = new TreeMap<>();

        TreeMap<Integer, TreeSet<DFSEdge>> joinBackCands = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> joinForCands = new TreeMap<>();
        joinCands(p, joinBackCands, joinForCands);

        TreeSet<DFSEdge> extendCands = new TreeSet<>();
        extendCands(p, extendCands);

        for (Cluster c : p.clustersAfterDelimiter()) {
            joinExtendInter(c, p, joinBackCands, joinForCands, extendCands, addedChildren);
            joinExtendDelta(c, p, joinBackCands, joinForCands, extendCands, addedChildren);
        }
        for (LabeledGraph g : p.unClusteredGraphsAfterDelimiter()) {
            joinExtendOther(g, p, joinBackCands, joinForCands, extendCands, addedChildren);
        }

        return new ArrayList<>(addedChildren.values());
    }

    private void joinExtendInter(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands, TreeMap<DFSEdge, Pattern> addedChildren) {
        LabeledGraph inter = c.intersection();
        List<Embedding> interEmbeddings = p.intersectionEmbeddings(c);
        List<Embedding> borderEmbeddings = p.borderEmbeddings(c);
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);

        for (int interEmCount = 0, borderEmCount = 0 - interEmbeddings.size(); interEmCount < interEmbeddings.size() || borderEmCount < borderEmbeddings.size(); interEmCount++, borderEmCount++) {
            Embedding em;
            if (borderEmCount < 0) {
                em = interEmbeddings.get(interEmCount);
            } else {
                em = borderEmbeddings.get(borderEmCount);
            }
            List<LabeledVertex> emVertices = em.vertices();
            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                if (cands.contains(dfsEdge)) {
                    Pattern child = p.child(dfsEdge);
                    if (borderEmCount < 0) {
                        child.addIntersectionEmbedding(c, em);
                    } else {
                        child.addBorderEmbedding(c, em);
                    }
                    addedChildren.put(child.edge(), child);
                }
            }


            //join forward edge
            BitSet emBits = new BitSet(maxVid + 1);
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }

            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertices.get(entry.getKey());
                for (LabeledEdge e : inter.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), inter.vLabel(from), inter.vLabel(e.to()), inter.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);;
                        if (borderEmCount < 0) {
                            child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                        } else {
                            child.addBorderEmbedding(c, new Embedding(e.to(), em));
                        }
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            //extend backward edges
            LabeledVertex from = emVertices.get(rmDfsId);
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                LabeledEdge pathEdge = inter.edge(to.id(), nextTo.id());
                if (inter.eLabel(pathEdge) > inter.eLabel(back) || (inter.eLabel(pathEdge) == inter.eLabel(back) && inter.vLabel(nextTo) > inter.vLabel(back.from()))) {
                    continue;
                }

                DFSEdge dfsEdge;
                if (inter.vLabel(from) <= inter.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                } else {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(back));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, toId, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                    if (borderEmCount < 0) {
                        child.addIntersectionEmbedding(c, em);
                    } else {
                        child.addBorderEmbedding(c, em);
                    }
                    addedChildren.put(child.edge(), child);
                }
            }

            //extend forward edges
            for (LabeledEdge e : inter.adjEdges(from.id())) {
                LabeledVertex to = e.to();
                if (emBits.get(to.id())) {
                    continue;
                }
                DFSEdge dfsEdge;
                if (inter.vLabel(from) <= inter.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));
                } else {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(e));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, emVertices.size(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));;
                    if (borderEmCount < 0) {
                        child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                    } else {
                        child.addBorderEmbedding(c, new Embedding(e.to(), em));
                    }
                    addedChildren.put(child.edge(), child);
                }
            }

        }
    }

    private void joinExtendDelta(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands, TreeMap<DFSEdge, Pattern> addedChildren) {
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
        for (LabeledGraph g : c) {
            Cluster.DeltaGraph dg = c.deltaGraph(g);

            for (Embedding em : p.borderEmbeddings(c)) {
                if (!isBorderEmbedding(em, dg.border())) {
                    continue;
                }
                List<LabeledVertex> emVertices = em.vertices();
                //join backward edges
                for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                    LabeledVertex from = emVertices.get(emVertices.size() - 1);
                    LabeledVertex to = emVertices.get(entry.getKey());
                    LabeledEdge back = dg.edge(from.id(), to.id());
                    if (back == null) {
                        continue;
                    }
                    TreeSet<DFSEdge> cands = entry.getValue();
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, em);
                        addedChildren.put(child.edge(), child);
                    }
                }

                //join forward edge
                BitSet emBits = new BitSet(maxVid + 1);
                for (LabeledVertex v : emVertices) {
                    emBits.set(v.id());
                }

                for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                    LabeledVertex from = emVertices.get(entry.getKey());
                    for (LabeledEdge e : dg.adjEdges(from.id())) {
                        if (emBits.get(e.to().id())) {
                            continue;
                        }
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), dg.vLabel(from), dg.vLabel(e.to()), dg.eLabel(e));
                        TreeSet<DFSEdge> cands = entry.getValue();
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addEmbedding(g, new Embedding(e.to(), em));
                            addedChildren.put(child.edge(), child);
                        }
                    }
                }

                //extend
                //extend backward edges
                LabeledVertex from = emVertices.get(rmDfsId);
                for (int j = 0; j < rmPathIds.size() - 2; j++) {
                    int toId = rmPathIds.get(j);
                    LabeledVertex to = emVertices.get(toId);
                    LabeledEdge back = dg.edge(from.id(), to.id());
                    if (back == null) {
                        continue;
                    }
                    LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                    LabeledEdge pathEdge = g.edge(to.id(), nextTo.id());
                    if (g.eLabel(pathEdge) > g.eLabel(back) || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
                        continue;
                    }

                    DFSEdge dfsEdge;
                    if (dg.vLabel(from) <= dg.vLabel(to)) {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                    } else {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(to), dg.vLabel(from), dg.eLabel(back));
                    }
                    if (extendCands.contains(dfsEdge)) {
                        Pattern child = p.child(rmDfsId, toId, dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                        child.addEmbedding(g, em);
                        addedChildren.put(child.edge(), child);
                    }
                }

                //extend forward edges
                for (LabeledEdge e : dg.adjEdges(from.id())) {
                    LabeledVertex to = e.to();
                    if (emBits.get(to.id())) {
                        continue;
                    }
                    DFSEdge dfsEdge;
                    if (dg.vLabel(from) <= dg.vLabel(to)) {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(from), dg.vLabel(to), dg.eLabel(e));
                    } else {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(to), dg.vLabel(from), dg.eLabel(e));
                    }
                    if (extendCands.contains(dfsEdge)) {
                        Pattern child = p.child(rmDfsId, emVertices.size(), dg.vLabel(from), dg.vLabel(to), dg.eLabel(e));
                        child.addEmbedding(g, new Embedding(to, em));
                        addedChildren.put(child.edge(), child);
                    }
                }
            }
        }
    }

    private void joinExtendOther(LabeledGraph g, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands, TreeMap<DFSEdge, Pattern> addedChildren) {
        List<Embedding> embeddings = p.embeddings(g);
        if (embeddings == null) {
            return;
        }
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
        for (Embedding em : embeddings) {
            List<LabeledVertex> emVertices = em.vertices();

            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                LabeledEdge back = g.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }

                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
                if (cands.contains(dfsEdge)) {
                    Pattern child = p.child(dfsEdge);
                    child.addEmbedding(g, em);
                    addedChildren.put(child.edge(), child);
                }
            }

            //join forward edges
            BitSet emBits = new BitSet(maxVid + 1);
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertices.get(entry.getKey());
                for (LabeledEdge e : g.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }

                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, new Embedding(e.to(), em));
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            //extend
            //extend backward edges
            LabeledVertex from = emVertices.get(rmDfsId);
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledEdge back = g.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                LabeledEdge pathEdge = g.edge(to.id(), nextTo.id());
                if (g.eLabel(pathEdge) > g.eLabel(back) || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
                    continue;
                }

                DFSEdge dfsEdge;
                if (g.vLabel(from) <= g.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                } else {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(back));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, toId, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                    child.addEmbedding(g, em);
                    addedChildren.put(child.edge(), child);
                }
            }

            //extend rm forward edges
            for (LabeledEdge e : g.adjEdges(from.id())) {
                LabeledVertex to = e.to();
                if (emBits.get(to.id())) {
                    continue;
                }

                DFSEdge dfsEdge;
                if (g.vLabel(from) <= g.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                } else {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(e));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, emVertices.size(), g.vLabel(from), g.vLabel(to), g.eLabel(e));
                    child.addEmbedding(g, new Embedding(to, em));
                    addedChildren.put(child.edge(), child);
                }
            }

        }
    }

    private void joinCands(Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand) {
        DFSEdge e1 = p.edge();
        for (Pattern sib : p.rightSiblings()) {
            DFSEdge e2 = sib.edge();
            if (e1.compareTo(e2) > 0) {
                continue;
            }

            if (!e1.isForward() && !e2.isForward() && e1.to() == e2.to()) {
                continue;
            }

            TreeSet<DFSEdge> candidates;
            if (!e2.isForward()) {
                candidates = backCand.computeIfAbsent(e2.to(), vIndex -> new TreeSet<>());
                candidates.add(e2);
            } else {
                candidates = forCand.computeIfAbsent(e2.from(), vIndex -> new TreeSet<>());
                candidates.add(new DFSEdge(e2.from(), p.code().nodeCount(), e2.fromLabel(), e2.toLabel(), e2.edgeLabel()));
            }
        }
    }

    private void extendCands(Pattern p, TreeSet<DFSEdge> extendCands) {
        DFSEdge lastEdge = p.edge();
        if (!lastEdge.isForward()) {
            return;
        }
        DFSEdge firstEdge = p.code().get(0);
        for (Pattern ep : this.points.get(firstEdge.fromLabel()).children()) {
            DFSEdge e = ep.edge();
            if (e.toLabel() == lastEdge.toLabel() && e.edgeLabel() >= firstEdge.edgeLabel()) {
                extendCands.add(e);
            }
        }
        for (PointPattern pp : this.points.tailMap(firstEdge.fromLabel(), false).headMap(lastEdge.toLabel()).values()) {
            for (Pattern ep : pp.children()) {
                DFSEdge e = ep.edge();
                if (e.toLabel() == lastEdge.toLabel()) {
                    extendCands.add(e);
                }
            }
        }
        PointPattern rmPoint = this.points.get(lastEdge.toLabel());
        if (rmPoint != null) {
            for (Pattern ep : rmPoint.children()) {
                extendCands.add(ep.edge());
            }
        }
    }

    private boolean isFrequent(Pattern p) {
        return p.support() >= this.absSup;
    }

    private void saveResult() {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(out);
            bw = new BufferedWriter(fw);
            for (PointPattern pp : points.values()) {
                if (!isFrequent(pp)) {
                    continue;
                }
                pointCount++;
                bw.write("t # " + (this.patternCount++) + " * " + pp.support());
                bw.newLine();
                bw.write("v 0 " + pp.label());
                bw.newLine();
                bw.newLine();

                for (Pattern child : pp.children()) {
                    if (!isFrequent(child) || !child.checkMin()) {
                        continue;
                    }
                    save(child, bw);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bw.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void save(Pattern p, BufferedWriter bw) throws IOException {
        bw.write("t # " + (this.patternCount++) + " * " + p.support());
        bw.newLine();
        DFSCode code = p.code();
        LabeledGraph g = code.toGraph();
        for (int i = 0; i < g.vSize(); i++) {
            LabeledVertex v = g.vertex(i);
            bw.write("v " + i + " " + g.vLabel(v));
            bw.newLine();
        }
        for (DFSEdge e : code.edges()) {
            bw.write("e " + e.from() + " " + e.to() + " " + e.edgeLabel());
            bw.newLine();
        }
        bw.newLine();

        for (Pattern child : p.children()) {
            if (!isFrequent(child) || !child.checkMin()) {
                continue;
            }
            save(child, bw);
        }
    }

    public void setOutput(File out) {
        this.out = out;
    }

}
