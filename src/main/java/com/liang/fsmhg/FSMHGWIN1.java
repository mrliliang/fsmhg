package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

import java.io.File;
import java.util.*;

public class FSMHGWIN1 {
    private TreeMap<Integer, PointPattern> points;
    private TreeMap<Long, LabeledGraph> trans;
    private File data;
    private File outdir;
    private double minSup;
    private double absSup;
    private int maxEdgeSize = Integer.MAX_VALUE;
    private boolean partition;
    private double similarity;
    int windowSize;
    int stepSize;
    private ArrayList<Cluster> clusters;
    private int clusterCounter;

    private int maxVid = 0;

    private LabeledGraph transDelimiter;
    private Cluster clusterDelimiter;


    public FSMHGWIN1(File data, File outdir, double minSupport, int maxEdgeSize, boolean partition, double similarity, int windowSize, int stepSize) {
        this.data = data;
        this.outdir = outdir;
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.windowSize = windowSize;
        this.stepSize = stepSize;
        this.points = new TreeMap<>();
        this.clusters = new ArrayList<>();
    }

    public void enumerate(TreeMap<Long, LabeledGraph> newTrans) {
        TreeMap<Long, LabeledGraph> removed = new TreeMap<>(this.trans.headMap(newTrans.firstKey()));
        TreeMap<Long, LabeledGraph> added = new TreeMap<>(newTrans.tailMap(this.trans.lastKey()));
        this.trans = newTrans;

        shrink(new ArrayList<>(removed.values()));
        grow(new ArrayList<>(added.values()));
    }

    private void shrink(List<LabeledGraph> graphs) {
        // TODO: 2020/4/3 shrink
        List<Cluster> changed = new ArrayList<>();
        Iterator<Cluster> it = this.clusters.iterator();
        while (it.hasNext()) {
            Cluster c = it.next();
            if (!c.remove(graphs)) {
                break;
            }
            // TODO: 2020/4/19 shrink is incorrect
            if (c.size() == 0) {
                changed.add(c);
            }
        }

        for (PointPattern pp : this.points.values()) {
            pp.remove(graphs, changed);
        }
    }

    private void grow(List<LabeledGraph> added) {
        transDelimiter = added.get(0);
        List<Cluster> addedClusters;
        Map<Integer, PointPattern> addedPoints;
        Map<DFSEdge, Pattern> addedEdges;
        if (partition) {
            addedClusters = Cluster.partition(added, similarity, clusterCounter);
            clusterDelimiter = addedClusters.get(0);
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
        Cluster.Intersection inter = c.intersection();
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
        Cluster.Intersection inter = c.intersection();

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
        }
    }

    private List<Pattern> enumerateChildren(Pattern p) {
        TreeMap<DFSEdge, Pattern> addedChildren = new TreeMap<>();

        TreeMap<Integer, TreeSet<DFSEdge>> joinBackCands = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> joinForCands = new TreeMap<>();
        joinCands(p, joinBackCands, joinForCands);

        TreeSet<DFSEdge> extendCands = new TreeSet<>();
        extendCands(p, extendCands);

        List<Cluster> clusters;
        List<LabeledGraph> graphs;
        if (p.hasChild()) {
            clusters = p.clusters(clusterDelimiter);
            graphs = p.unClusteredGraphs(transDelimiter);
        } else {
            clusters = p.clusters();
            graphs = p.unClusteredGraphs();
        }
        for (Cluster c : clusters) {
            joinExtendInter(c, p, joinBackCands, joinForCands, extendCands, addedChildren);
            joinExtendDelta(c, p, joinBackCands, joinForCands, extendCands, addedChildren);
        }
        for (LabeledGraph g : graphs) {
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
//            if (!isFrequent(sib)) {
//                continue;
//            }
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
//            if (!isFrequent(ep)) {
//                continue;
//            }
            DFSEdge e = ep.edge();
            if (e.toLabel() == lastEdge.toLabel() && e.edgeLabel() >= firstEdge.edgeLabel()) {
                extendCands.add(e);
            }
        }
        for (PointPattern pp : this.points.tailMap(firstEdge.fromLabel(), false).headMap(lastEdge.toLabel()).values()) {
//            if (!isFrequent(pp)) {
//                continue;
//            }
            for (Pattern ep : pp.children()) {
//                if (!isFrequent(ep)) {
//                    continue;
//                }
                DFSEdge e = ep.edge();
                if (e.toLabel() == lastEdge.toLabel()) {
                    extendCands.add(e);
                }
            }
        }
        PointPattern rmPoint = this.points.get(lastEdge.toLabel());
        if (rmPoint != null && isFrequent(rmPoint)) {
            for (Pattern ep : rmPoint.children()) {
//                if (!isFrequent(ep)) {
//                    continue;
//                }
                extendCands.add(ep.edge());
            }
        }
    }

    private boolean isFrequent(Pattern p) {
        return p.frequency() >= this.absSup;
    }

}